package com.example.flightsearchapp


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import com.example.flightsearchapp.preferenceFolder.PreferencesManager
import android.util.Log
import android.widget.Toast
import android.content.Context
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.flow.first
import androidx.activity.addCallback

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var searchEditText: TextInputEditText
    private lateinit var airportAdapter: suggestAirportAdapter
    private lateinit var airportRepository: FlightRepository
    private lateinit var flightAdapter: flightAdapter
    private lateinit var starredRep: starredRep
    private val searchJob = Job()
    private val searchScope = CoroutineScope(Dispatchers.Main + searchJob)
    private val flightJob = Job()
    private val flightScope = CoroutineScope(Dispatchers.Main + flightJob)
    private lateinit var faveAdapter: faveAdapter
    private val favoriteJob = Job()
    private val favoriteScope = CoroutineScope(Dispatchers.Main + favoriteJob)
    private var currentScrollPosition = 0
    private enum class DisplayState {
        FAVORITES,
        SEARCH_RESULTS,
        FLIGHTS
    }
    private var currentDisplayState = DisplayState.FAVORITES

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDependencies()
        setupRecyclerView()
        setupSearchView()
        setupFlightRecyclerView()
        setupFavorites()
        restoreAppState()
        setupBackNavigation()
    }

    private fun setupDependencies() {
        try {
            Log.d("MainActivity", "Initializing dependencies")
            preferencesManager = PreferencesManager(this)
            val database = FlightDatabase.getDatabase(this)
            Log.d("MainActivity", "Database initialized")

            airportRepository = FlightRepository(database.airportDao())
            starredRep = starredRep(database.favoriteDao())
            Log.d("MainActivity", "Repositories initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing dependencies", e)
            Toast.makeText(
                this,
                "Error initializing app: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        airportAdapter = suggestAirportAdapter { airport ->
            handleAirportSelection(airport)
        }
        recyclerView.apply {
            adapter = airportAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupSearchView() {
        searchEditText = findViewById(R.id.airport_search)

        // Restore saved search query
        lifecycleScope.launch {
            preferencesManager.searchQuery.collect { savedQuery ->
                if (searchEditText.text.toString() != savedQuery) {
                    searchEditText.setText(savedQuery)
                }
            }
        }

        // Setup search text watcher
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchAirports(s?.toString() ?: "")
            }
        })
    }

    private fun searchAirports(query: String) {
        if (query.isBlank()) {
            showFavorites()
            return
        }

        updateDisplayState(DisplayState.SEARCH_RESULTS)
        // Cancel previous search job
        searchScope.cancel()

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Searching for query: $query")
                // Clear favorites adapter first
                faveAdapter.submitList(emptyList())

                airportRepository.searchAirports(query).collect { airports ->
                    Log.d("MainActivity", "Found ${airports.size} airports")
                    if (airports.isEmpty()) {
                        Log.d("MainActivity", "No airports found")
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        // Set the airport adapter and its data
                        findViewById<RecyclerView>(R.id.search_results).adapter =
                            airportAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
                        airportAdapter.submitList(airports)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching airports", e)
            }
        }
    }

    private fun showEmptyState() {
        findViewById<TextView>(R.id.empty_state).apply {
            visibility = View.VISIBLE
            text = getString(R.string.no_results)
        }
        findViewById<RecyclerView>(R.id.search_results).visibility = View.GONE
    }

    private fun hideEmptyState() {
        findViewById<TextView>(R.id.empty_state).visibility = View.GONE
        findViewById<RecyclerView>(R.id.search_results).visibility = View.VISIBLE
    }

    private fun handleAirportSelection(selectedAirport: Airport) {
        updateDisplayState(DisplayState.FLIGHTS)
        // Hide keyboard and clear search
        searchEditText.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(searchEditText.windowToken, 0)

        // Update title to show selected airport
        supportActionBar?.title = getString(R.string.flights_from, selectedAirport.iataCode)

        // Clear other adapters first
        airportAdapter.submitList(emptyList())
        faveAdapter.submitList(emptyList())

        // Set the flight adapter
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        recyclerView.adapter = flightAdapter

        // Load and display flights
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Loading flights from ${selectedAirport.iataCode}")
                airportRepository.getDestinationAirports(selectedAirport.iataCode)
                    .collect { destinations ->
                        Log.d("MainActivity", "Found ${destinations.size} destinations")
                        val flights = destinations.map { destination ->
                            flight(
                                depAirport = selectedAirport,
                                destAirport = destination,
                                isFavorite = false // Will be updated by the collect below
                            )
                        }
                        flightAdapter.submitList(flights)

                        // Check favorite status for each flight
                        flights.forEach { flight ->
                            starredRep.faveRoute(
                                flight.depAirport.iataCode,
                                flight.destAirport.iataCode
                            ).collect { isFavorite ->
                                flight.isFavorite = isFavorite
                                val position = flightAdapter.currentList.indexOf(flight)
                                if (position != -1) {
                                    flightAdapter.notifyItemChanged(position)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading flights", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading flights: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showFavorites() {
        updateDisplayState(DisplayState.FAVORITES)
        // Clear other adapters first
        airportAdapter.submitList(emptyList())
        flightAdapter.submitList(emptyList())

        // Set the favorite adapter immediately
        findViewById<RecyclerView>(R.id.search_results).adapter =
            faveAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>

        // Start observing favorites
        favoriteScope.launch {
            starredRep.Fave().collect { favorites ->
                if (favorites.isEmpty()) {
                    showEmptyFavorites()
                } else {
                    hideEmptyState()
                    // Load airport information for each favorite
                    favorites.forEach { favorite ->
                        favorite.departureAirport = airportRepository.getAirportByCode(favorite.departureCode)
                        favorite.destinationAirport = airportRepository.getAirportByCode(favorite.destinationCode)
                    }
                    faveAdapter.submitList(favorites)
                }
            }
        }
    }

    private fun showEmptyFavorites() {
        // Show empty state message
        findViewById<TextView>(R.id.empty_state).apply {
            visibility = View.VISIBLE
            text = getString(R.string.no_favorites)
        }
    }

    private fun handleFavoriteDelete(fave: fave) {
        favoriteScope.launch {
            starredRep.remFave(fave)
            // After deleting the last fave, check if the list is empty and update UI
            val remainingFavorites = starredRep.Fave().first()
            if (remainingFavorites.isEmpty()) {
                withContext(Dispatchers.Main) {
                    updateEmptyState(true)
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        findViewById<TextView>(R.id.empty_state).visibility = if (isEmpty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.search_results).visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showFlightResults(departure: Airport, destinations: List<Airport>) {
        flightScope.launch {
            val flights = destinations.map { destination ->
                flight(departure, destination).also { flight ->
                    starredRep.faveRoute(
                        flight.depAirport.iataCode,
                        flight.destAirport.iataCode
                    ).collect { isFavorite ->
                        flight.isFavorite = isFavorite
                        flightAdapter.notifyItemChanged(flightAdapter.currentList.indexOf(flight))
                    }
                }
            }
            flightAdapter.submitList(flights)
        }
    }

    private fun setupFlightRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        flightAdapter = flightAdapter { flight ->
            handleFavoriteClick(flight)
        }
        recyclerView.apply {
            adapter = flightAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun handleFavoriteClick(flight: flight) {
        lifecycleScope.launch {
            // First check if it's favorite
            val isFavorite = starredRep.faveRoute(
                flight.depAirport.iataCode,
                flight.destAirport.iataCode
            ).first() // Use first() instead of collect to get single value

            if (isFavorite) {
                val favorite = starredRep.getFave(
                    flight.depAirport.iataCode,
                    flight.destAirport.iataCode
                )
                favorite?.let {
                    starredRep.remFave(it)
                    // Update UI immediately
                    flight.isFavorite = false
                    val position = flightAdapter.currentList.indexOf(flight)
                    if (position != -1) {
                        flightAdapter.notifyItemChanged(position)
                    }
                }
            } else {
                val newFave = fave(
                    departureCode = flight.depAirport.iataCode,
                    destinationCode = flight.destAirport.iataCode
                )
                starredRep.addFavorite(newFave)
                // Update UI immediately
                flight.isFavorite = true
                val position = flightAdapter.currentList.indexOf(flight)
                if (position != -1) {
                    flightAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun setupFavorites() {
        faveAdapter = faveAdapter { favorite ->
            handleFavoriteDelete(favorite)
        }

        // Show favorites initially if search is empty
        if (searchEditText.text.isNullOrEmpty()) {
            showFavorites()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob.cancel()
        flightJob.cancel()
        favoriteJob.cancel()
    }

    override fun onPause() {
        super.onPause()
        saveAppState()
    }

    private fun saveAppState() {
        lifecycleScope.launch {
            val recyclerView = findViewById<RecyclerView>(R.id.search_results)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            currentScrollPosition = layoutManager.findFirstVisibleItemPosition()

            preferencesManager.apply {
                saveSearchQuery(searchEditText.text?.toString() ?: "")
                saveScrollPosition(currentScrollPosition)
            }
        }
    }

    private fun restoreAppState() {
        lifecycleScope.launch {
            // Restore scroll position
            preferencesManager.scrollPosition.collect { position ->
                val recyclerView = findViewById<RecyclerView>(R.id.search_results)
                (recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPosition(position)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("SCROLL_POSITION", currentScrollPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentScrollPosition = savedInstanceState.getInt("SCROLL_POSITION", 0)
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)
        recyclerView.layoutManager?.scrollToPosition(currentScrollPosition)
    }

    private fun updateDisplayState(newState: DisplayState) {
        if (currentDisplayState != newState) {
            currentDisplayState = newState
            // Clear all adapters
            airportAdapter.submitList(emptyList())
            faveAdapter.submitList(emptyList())
            flightAdapter.submitList(emptyList())
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                // If there's text in search, clear it and show favorites
                !searchEditText.text.isNullOrEmpty() -> {
                    searchEditText.setText("")
                    showFavorites()
                }
                // If we're showing flights, go back to favorites
                currentDisplayState == DisplayState.FLIGHTS -> {
                    showFavorites()
                }
                // Otherwise, allow normal back behavior (exit app)
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}