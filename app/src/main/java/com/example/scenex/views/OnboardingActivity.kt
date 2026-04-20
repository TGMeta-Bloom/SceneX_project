package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.scenex.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        val nextButton = findViewById<TextView>(R.id.nextButton)
        val skipTextView = findViewById<TextView>(R.id.skipTextView)
        val backTextView = findViewById<TextView>(R.id.backTextView)

        val onboardingItems = listOf(
            OnboardingItem(
                "Welcome to SceneX",
                "Casting the Future of Entertainment",
                R.drawable.welcome1
            ),
            OnboardingItem(
                "The Ultimate Spotlight for Every Talent",
                "The bridge between world-class production and elite talent",
                R.drawable.welcome2
            ),
            OnboardingItem(
                "Eliminate Guesswork with Role-Fit Scoring",
                "Smart matching. Zero scrolling. The right talent, ranked for you.",
                R.drawable.welcome3
            ),
            OnboardingItem(
                "From Discovery to Set in Record Time",
                "Spend less time on paperwork and more time on performance. We'll handle the rest.",
                R.drawable.welcome4
            )
        )

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                navigateToRoleSelect()
            }
        }

        skipTextView.setOnClickListener {
            navigateToRoleSelect()
        }

        backTextView.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                backTextView.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
                skipTextView.visibility = if (position == onboardingItems.size - 1) View.INVISIBLE else View.VISIBLE
                
                if (position == onboardingItems.size - 1) {
                    nextButton.text = "Get Started"
                } else {
                    nextButton.text = "Next"
                }
            }
        })
    }

    private fun navigateToRoleSelect() {
        startActivity(Intent(this, RoleSelectActivity::class.java))
        finish()
    }

    data class OnboardingItem(val title: String, val description: String, val imageRes: Int)

    class OnboardingAdapter(private val items: List<OnboardingItem>) :
        RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

        class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image = view.findViewById<ImageView>(R.id.onboardingImage)
            val title = view.findViewById<TextView>(R.id.onboardingTitle)
            val description = view.findViewById<TextView>(R.id.onboardingDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.description.text = item.description
            holder.image.setImageResource(item.imageRes)
            holder.image.clearColorFilter()
        }

        override fun getItemCount() = items.size
    }
}
