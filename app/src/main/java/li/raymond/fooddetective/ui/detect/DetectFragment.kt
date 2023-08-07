package li.raymond.fooddetective.ui.detect

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import li.raymond.fooddetective.MainActivity.Companion.FILENAME_FORMAT
import li.raymond.fooddetective.MainActivity.Companion.TAG
import li.raymond.fooddetective.databinding.FragmentDetectBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.tasks.await
import li.raymond.fooddetective.BuildConfig
import li.raymond.fooddetective.MainActivity
import li.raymond.fooddetective.R

class DetectFragment : Fragment() {

    private var _binding: FragmentDetectBinding? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val OPENAI_API_KEY = BuildConfig.OPENAI_KEY

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the listeners for take photo and video capture buttons
        _binding!!.imageCaptureButton.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        return root
    }

    private suspend fun ocrPhoto(photoURI: Uri): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(requireContext(), photoURI)
        var res = ""
        return try {
            val visionText = recognizer.process(image).await()
            visionText.textBlocks.forEach { block ->
                block.lines.forEach { line ->
                    res += line.text + " "
                }
            }
            Log.d(TAG, "OCR success, result: $res")
            res
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed: ${e.message}", e)
            ""
        }
    }

    @OptIn(BetaOpenAI::class)
    private suspend fun chatGPTit(input: String): String {
        val instructions = """You will be provided with a list of food ingredient names.
For each ingredient with a chemical name, respond with a description its purpose and a description of its health effects.
Make sure to emphasize any risks of cancer or organ damage.
Each ingredient result should be about 30 words."""
        val openai = OpenAI(token = OPENAI_API_KEY)
        val response = openai.chatCompletion(
            ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = instructions
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = input
                    )
                ),
                temperature = 0.4,
                maxTokens = 120 * input.split(',').size,
                topP = 1.0,
                frequencyPenalty = 0.0,
                presencePenalty = 0.0
            )
        )
        return response.choices[0].message?.content ?: ""
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FoodDetective")
//            }
//        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            File(requireContext().filesDir, "captures/$name.jpg")
        ).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val baseContext = requireContext()
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    // OCR photo
                    runBlocking {
                        launch {
                            val text = ocrPhoto(output.savedUri!!)
                            Log.d(TAG, "OCR result: $text")
                            Toast.makeText(baseContext, text, Toast.LENGTH_LONG).show()
                            val res = chatGPTit(text)
                            Log.d(TAG, "ChatGPT result: $res")

                            // Save result to file
                            val file = File(baseContext.filesDir, "captures/$name.txt")
                            file.writeText(res)

                            // Redirect to gallery fragment
                            (context as MainActivity).findNavController(R.id.nav_host_fragment_content_main)
                                .navigate(R.id.nav_history)
                        }
                    }
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

        imageCapture = ImageCapture.Builder().build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
