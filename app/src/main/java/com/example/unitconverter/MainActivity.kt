@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.unitconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*  // ✅ you can still use Material Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource   // ✅ for loading drawable images
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnitConverterTheme {
                var showSplash by remember { mutableStateOf(true) } // NEW

                if (showSplash) {
                    SplashScreen { showSplash = false } // NEW
                } else {
                    UnitConverterApp()
                }
            }
        }
    }
}

@Composable
fun UnitConverterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        content()
    }
}

// ----------------- SPLASH SCREEN -----------------

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Fade-in animation
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500), label = ""
    )

    // Trigger
    LaunchedEffect(true) {
        startAnimation = true
        delay(2500) // Show splash for 2.5 sec
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.unithub_logo), // put your generated logo in res/drawable
            contentDescription = "UnitHub Logo",
            modifier = Modifier
                .size(180.dp)
                .alpha(alphaAnim.value)
        )

    }
}
// ----------------- MAIN APP -----------------

@Composable
fun UnitConverterApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        UnitCategories.categories.forEach { category ->
            composable(category.name) {
                ConverterScreen(category, navController)
            }
        }
    }
}

// ----------------- DASHBOARD -----------------

@Composable
fun DashboardScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Unit Converter", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(UnitCategories.categories) { category ->
                val scale = remember { Animatable(1f) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                        .clickable {
                            navController.navigate(category.name)
                        },
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            category.icon,
                            contentDescription = category.name,
                            tint = category.color,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ----------------- CONVERTER SCREEN -----------------

@Composable
fun ConverterScreen(category: Category, navController: NavHostController) {
    val scope = rememberCoroutineScope() // ✅ coroutine scope

    var inputValue by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf(category.units.first()) }
    var toUnit by remember { mutableStateOf(category.units[1]) }
    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${category.name} Converter") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background faded icon
            Icon(
                category.icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .alpha(0.05f),
                tint = category.color
            )

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Enter value") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DropdownSelector("From", fromUnit, category.units) { fromUnit = it }
                    DropdownSelector("To", toUnit, category.units) { toUnit = it }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Convert Button
                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            result = null
                            delay(1500) // simulate loading

                            result = try {
                                val value = inputValue.toDouble()
                                val converted = category.converter(value, fromUnit, toUnit)
                                "$inputValue $fromUnit = $converted $toUnit"
                            } catch (e: Exception) {
                                "Invalid input"
                            }

                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Convert")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Result
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    result?.let {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------- DROPDOWN SELECTOR -----------------

@Composable
fun DropdownSelector(label: String, selected: String, items: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ----------------- DATA + LOGIC -----------------

data class Category(
    val name: String,
    val units: List<String>,
    val converter: (Double, String, String) -> Double,
    val icon: ImageVector,
    val color: Color
)

object UnitCategories {
    val categories = listOf(

        // LENGTH
        Category("Length",
            listOf("m","cm","mm","µm","nm","pm","fm","dm","dam","hm","km","Mm","Gm","Tm","Pm","light year","ft","in"),
            { v, f, t ->
                val factors = mapOf(
                    "m" to 1.0, "cm" to 0.01, "mm" to 0.001, "µm" to 1e-6, "nm" to 1e-9, "pm" to 1e-12, "fm" to 1e-15,
                    "dm" to 0.1, "dam" to 10.0, "hm" to 100.0, "km" to 1000.0, "Mm" to 1e6, "Gm" to 1e9, "Tm" to 1e12,
                    "Pm" to 1e15, "light year" to 9.461e15, "ft" to 0.3048, "in" to 0.0254
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Straighten, Color.Green
        ),

        // AREA
        Category("Area",
            listOf("m²","cm²","mm²","km²","ha","acre","ft²","in²","yd²"),
            { v, f, t ->
                val factors = mapOf(
                    "m²" to 1.0, "cm²" to 0.0001, "mm²" to 1e-6, "km²" to 1e6,
                    "ha" to 10000.0, "acre" to 4046.86, "ft²" to 0.092903, "in²" to 0.00064516, "yd²" to 0.836127
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.SquareFoot, Color.Blue
        ),

        // VOLUME
        Category("Volume",
            listOf("m³","cm³","mm³","L","mL","dm³","ft³","in³","yd³","gal","cup","tbsp"),
            { v, f, t ->
                val factors = mapOf(
                    "m³" to 1.0, "cm³" to 1e-6, "mm³" to 1e-9, "L" to 0.001, "mL" to 1e-6, "dm³" to 0.001,
                    "ft³" to 0.0283168, "in³" to 1.6387e-5, "yd³" to 0.764555, "gal" to 0.00378541,
                    "cup" to 0.000236588, "tbsp" to 1.4787e-5
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.LocalDrink, Color.Cyan
        ),

        // MASS
        Category("Mass",
            listOf("mg","g","kg","t","oz","lb","carat","quintal"),
            { v, f, t ->
                val factors = mapOf(
                    "mg" to 1e-6, "g" to 0.001, "kg" to 1.0, "t" to 1000.0,
                    "oz" to 0.0283495, "lb" to 0.453592, "carat" to 0.0002, "quintal" to 100.0
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Scale, Color.Magenta
        ),

        // TIME
        Category("Time",
            listOf("ns","µs","ms","s","min","h","day","year"),
            { v, f, t ->
                val factors = mapOf(
                    "ns" to 1e-9, "µs" to 1e-6, "ms" to 1e-3, "s" to 1.0,
                    "min" to 60.0, "h" to 3600.0, "day" to 86400.0, "year" to 31536000.0
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.AccessTime, Color.Yellow
        ),

        // TEMPERATURE
        Category("Temperature", listOf("°C","°F","K"),
            { v, f, t ->
                when (f to t) {
                    "°C" to "°F" -> v * 9/5 + 32
                    "°F" to "°C" -> (v - 32) * 5/9
                    "°C" to "K" -> v + 273.15
                    "K" to "°C" -> v - 273.15
                    "°F" to "K" -> (v - 32) * 5/9 + 273.15
                    "K" to "°F" -> (v - 273.15) * 9/5 + 32
                    else -> v
                }
            },
            Icons.Filled.Thermostat, Color.Red
        ),

        // SPEED
        Category("Speed",
            listOf("m/s","km/h","mph","knot","ft/s","c [speed of light]"),
            { v, f, t ->
                val factors = mapOf(
                    "m/s" to 1.0, "km/h" to 0.277778, "mph" to 0.44704,
                    "knot" to 0.514444, "ft/s" to 0.3048, "c [speed of light]" to 299792458.0
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Speed, Color.LightGray
        ),

        // ENERGY
        Category("Energy",
            listOf("J","kJ","cal","kcal","Wh","kWh","erg","BTU","eV"),
            { v, f, t ->
                val factors = mapOf(
                    "J" to 1.0, "kJ" to 1000.0, "cal" to 4.184, "kcal" to 4184.0,
                    "Wh" to 3600.0, "kWh" to 3.6e6, "erg" to 1e-7,
                    "BTU" to 1055.06, "eV" to 1.602e-19
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Bolt, Color.Yellow
        ),

        // POWER
        Category("Power",
            listOf("W","kW","MW","GW","cal/s","BTU/h"),
            { v, f, t ->
                val factors = mapOf(
                    "W" to 1.0, "kW" to 1000.0, "MW" to 1e6, "GW" to 1e9,
                    "cal/s" to 4.184, "BTU/h" to 0.293071
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.ElectricBolt, Color.Green
        ),

        // PRESSURE
        Category("Pressure",
            listOf("Pa","kPa","MPa","bar","atm","mmHg","psi","Torr"),
            { v, f, t ->
                val factors = mapOf(
                    "Pa" to 1.0, "kPa" to 1000.0, "MPa" to 1e6,
                    "bar" to 1e5, "atm" to 101325.0, "mmHg" to 133.322,
                    "psi" to 6894.76, "Torr" to 133.322
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Compress, Color.Cyan
        ),

        // FORCE
        Category("Force",
            listOf("N","kN","dyne","kgf"),
            { v, f, t ->
                val factors = mapOf(
                    "N" to 1.0, "kN" to 1000.0, "dyne" to 1e-5, "kgf" to 9.80665
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.FitnessCenter, Color.Blue
        ),

        // DATA STORAGE
        Category("Data Storage",
            listOf("bit","byte","KB","MB","GB","TB"),
            { v, f, t ->
                val factors = mapOf(
                    "bit" to 1.0,
                    "byte" to 8.0,
                    "KB" to 8.0 * 1024,
                    "MB" to 8.0 * 1024 * 1024,
                    "GB" to 8.0 * 1024 * 1024 * 1024,
                    "TB" to 8.0 * 1024 * 1024 * 1024 * 1024
                )
                v * (factors[f]!! / factors[t]!!)
            },
            Icons.Filled.Storage, Color.Gray
        )
    )
}
