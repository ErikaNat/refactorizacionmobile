class MainActivity : AppCompatActivity() {
    private var productList = mutableListOf<Product>()
    private var totalAmount = 0.0
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var totalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupRecyclerView()
        setupButtonListener()
        loadProducts()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        totalTextView = findViewById(R.id.totalTextView)
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList) { productName, productPrice ->
            addProduct(Product(name = productName, price = productPrice, quantity = 1))
        }
        recyclerView.adapter = adapter
    }
    
    private fun setupButtonListener() {
        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            loadProducts()
        }
    }
    
    private fun loadProducts() {
        showLoading()
        val database = FirebaseFirestore.getInstance()
        database.collection("products").get()
            .addOnSuccessListener { documents ->
                productList.clear()
                documents.forEach { document ->
                    val product = Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        quantity = document.getLong("quantity")?.toInt() ?: 1
                    )
                    productList.add(product)
                }
                adapter.notifyDataSetChanged()
                calculateTotal()
                hideLoading()
            }
            .addOnFailureListener { exception ->
                showErrorMessage(exception.message)
                hideLoading()
            }
    }
    
    private fun addProduct(product: Product) {
        productList.add(product)
        adapter.notifyDataSetChanged()
        calculateTotal()
    }
    
    private fun calculateTotal() {
        totalAmount = productList.sumOf { it.price * it.quantity }
        totalTextView.text = getString(R.string.total_format, totalAmount)
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }
    
    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }
    
    private fun showErrorMessage(message: String?) {
        Toast.makeText(
            this, 
            getString(R.string.error_message, message ?: getString(R.string.unknown_error)),
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private inner class ProductAdapter(
        private val products: List<Product>,
        private val onProductAdd: (String, Double) -> Unit
    ) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
            val quantityTextView: TextView = itemView.findViewById(R.id.productQuantityTextView)
            val addButton: Button = itemView.findViewById(R.id.addButton)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product, parent, false)
            return ViewHolder(view)
        }
        
        override fun getItemCount() = products.size
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val product = products[position]
            holder.nameTextView.text = product.name
            holder.priceTextView.text = getString(R.string.price_format, product.price)
            holder.quantityTextView.text = getString(R.string.quantity_format, product.quantity)
            
            holder.addButton.setOnClickListener {
                val newQuantity = product.quantity + 1
                product.quantity = newQuantity
                holder.quantityTextView.text = getString(R.string.quantity_format, newQuantity)
                calculateTotal()
            }
        }
    }
    
    private data class Product(
        val id: String = "",
        val name: String,
        val price: Double,
        var quantity: Int
    )
}
