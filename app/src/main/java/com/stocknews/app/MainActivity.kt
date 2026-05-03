package com.stocknews.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocknews.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val adapter = NewsAdapter(::openArticle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { viewModel.refresh() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: MainViewModel.UiState) {
        when (state) {
            is MainViewModel.UiState.Loading -> {
                binding.swipeRefresh.isRefreshing = true
                binding.emptyGroup.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
            is MainViewModel.UiState.Content -> {
                binding.swipeRefresh.isRefreshing = false
                binding.emptyGroup.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(state.articles)
                if (state.errors.isNotEmpty()) {
                    showErrorToast(state.errors)
                }
            }
            is MainViewModel.UiState.Empty -> {
                binding.swipeRefresh.isRefreshing = false
                binding.recyclerView.visibility = View.GONE
                binding.emptyGroup.visibility = View.VISIBLE
                binding.emptyMessage.text = if (state.errors.isEmpty()) {
                    getString(R.string.empty_no_articles)
                } else {
                    getString(
                        R.string.empty_with_errors,
                        state.errors.joinToString { it.source.displayName }
                    )
                }
            }
        }
    }

    private fun openArticle(article: NewsArticle) {
        val uri = runCatching { Uri.parse(article.link) }.getOrNull() ?: return
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.error_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorToast(errors: List<SourceError>) {
        val sources = errors.joinToString { it.source.displayName }
        Toast.makeText(
            this,
            getString(R.string.error_partial_load, sources),
            Toast.LENGTH_SHORT
        ).show()
    }
}
