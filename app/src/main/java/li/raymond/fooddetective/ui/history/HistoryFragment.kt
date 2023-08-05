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
        val padding = resources.getDimensionPixelOffset(R.dimen.padding)

        // Sort files newest first
        if (files == null || files.isEmpty()) {
            listcontainer.addView(TextView(requireContext()).apply {
                text = context.getString(R.string.no_captures_yet)
                setPadding(padding, padding, 0, 0)
            })
        } else {
            files.sort()
            files.reverse()
            for (file in files) {
                val name = file.name
                if (name.endsWith(".jpg") || name.endsWith(".png")) {
                    listcontainer.addView(ImageView(requireContext()).apply {
                        setImageURI(file.toUri())
                        setPaddingRelative(padding, 0, padding, 0)
                    })
                } else if (name.endsWith(".txt")) {
                    listcontainer.addView(TextView(requireContext()).apply {
                        text = name.removeSuffix(".txt")
                        setTypeface(null, Typeface.BOLD)
                        setPaddingRelative(padding, padding, padding, padding)
                    })
                    val res = file.readText()
                    listcontainer.addView(TextView(requireContext()).apply {
                        text = res
                        setPadding(padding, padding / 5, padding, padding)
                    })
                }
            }

            // Add padding at the end
            listcontainer.addView(TextView(requireContext()).apply {
                setPadding(0, 0, 0, padding)
            })
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
