package com.example.flightsearchapp



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flightsearchapp.databinding.ItemFavoriteRouteBinding

class faveAdapter(
    private val onDeleteClick: (fave) -> Unit
) : ListAdapter<fave, faveAdapter.FavoriteViewHolder>(faveCallBack()) {

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fave: fave) {
            binding.apply {
                depCode.text = fave.departureCode
                depName.text = fave.departureAirport?.name ?: ""
                destCode.text = fave.destinationCode
                destName.text = fave.destinationAirport?.name ?: ""
                deleteButton.setOnClickListener { onDeleteClick(fave) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        return FavoriteViewHolder(
            ItemFavoriteRouteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class faveCallBack : DiffUtil.ItemCallback<fave>() {
        override fun areItemsTheSame(oldItem: fave, newItem: fave) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: fave, newItem: fave) =
            oldItem == newItem
    }
}