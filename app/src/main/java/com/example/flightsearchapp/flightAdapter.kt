package com.example.flightsearchapp



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flightsearchapp.databinding.ItemFlightBinding

class flightAdapter(
    private val onFavoriteClick: (flight) -> Unit
) : ListAdapter<flight, flightAdapter.FlightViewHolder>(FlightDiffCallback()) {

    inner class FlightViewHolder(
        private val binding: ItemFlightBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(flight: flight) {
            with(binding) {
                destName.text = flight.destAirport.name
                depCode.text = flight.depAirport.iataCode
                depName.text = flight.depAirport.name
                destCode.text = flight.destAirport.iataCode

                faveButton.apply {
                    setIconResource(
                        if (flight.isFavorite) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                    contentDescription = itemView.context.getString(
                        if (flight.isFavorite) R.string.remove_from_favorites
                        else R.string.add_to_favorites
                    )
                    setOnClickListener {
                        onFavoriteClick(flight)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightViewHolder {
        return FlightViewHolder(
            ItemFlightBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FlightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FlightDiffCallback : DiffUtil.ItemCallback<flight>() {
        override fun areItemsTheSame(oldItem: flight, newItem: flight): Boolean =
            oldItem.depAirport.id == newItem.depAirport.id &&
                    oldItem.destAirport.id == newItem.destAirport.id

        override fun areContentsTheSame(oldItem: flight, newItem: flight): Boolean =
            oldItem == newItem
    }
}