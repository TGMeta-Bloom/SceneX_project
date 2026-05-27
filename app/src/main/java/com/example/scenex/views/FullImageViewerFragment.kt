package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.scenex.R

/**
 * Technical Implementation: High-Resolution Asset Viewer.
 */
class FullImageViewerFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_URL = "arg_image_url"

        fun newInstance(imageUrl: String): FullImageViewerFragment {
            val fragment = FullImageViewerFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_full_image_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivFullImage = view.findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(ivFullImage)

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
