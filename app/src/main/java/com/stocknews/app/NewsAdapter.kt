package com.stocknews.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stocknews.app.databinding.ItemArticleBinding
import java.text.DateFormat
import java.util.Date

class NewsAdapter(
    private val onClick: (NewsArticle) -> Unit
) : ListAdapter<NewsArticle, NewsAdapter.ArticleViewHolder>(DIFF) {

    private val dateFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArticleBinding.inflate(inflater, parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: NewsArticle) {
            binding.title.text = article.title
            binding.source.text = article.source.displayName
            binding.timestamp.text = formatDate(article.publishedAt)

            val description = article.description
            binding.description.text = description
            binding.description.visibility =
                if (description.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.root.setOnClickListener { onClick(article) }
        }

        private fun formatDate(date: Date?): String =
            if (date == null) "" else dateFormat.format(date)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NewsArticle>() {
            override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean =
                oldItem.link == newItem.link

            override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean =
                oldItem == newItem
        }
    }
}
