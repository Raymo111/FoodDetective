package li.raymond.fooddetective.ui.history

import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import li.raymond.fooddetective.R
import li.raymond.fooddetective.databinding.FragmentHistoryBinding
import java.io.File

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val listcontainer: LinearLayout = binding.listcontainer
        listcontainer.removeAllViews()

        // Load all saved captures: captures/$name.txt
        val dir = requireContext().filesDir.path + "/captures"
        val files = File(dir).listFiles()

        // Sort files newest first
        if (files == null || files.isEmpty()) {
            listcontainer.addView(TextView(requireContext()).apply {
                text = context.getString(R.string.no_captures_yet)
                setPadding(50, 50, 0, 0)
            })
        } else {
            files.sort()
            files.reverse()
            for (file in files) {
                val name = file.name
                if (name.endsWith(".jpg") || name.endsWith(".png")) {
                    listcontainer.addView(ImageView(requireContext()).apply {
                        setImageURI(file.toUri())
                    })
                } else if (name.endsWith(".txt")) {
                    listcontainer.addView(TextView(requireContext()).apply {
                        text = name.removeSuffix(".txt")
                        setTypeface(null, Typeface.BOLD)
                        setPadding(50, 50, 50, 0)
                    })
                    val res = file.readText()
                    listcontainer.addView(TextView(requireContext()).apply {
                        text = res
                        setPadding(50, 10, 50, 50)
                    })
                }
            }

            // Add padding at the end
            listcontainer.addView(TextView(requireContext()).apply {
                setPadding(0, 0, 0, 50)
            })
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
