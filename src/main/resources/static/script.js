document.addEventListener('DOMContentLoaded', function() {
    // Get all required elements
    const categoriesDataElement = document.getElementById('categoriesData');
    const subCategoriesDataElement = document.getElementById('subCategoriesData');
    const categorySelect = document.getElementById('categorySelect');
    const subCategorySelect = document.getElementById('subCategorySelect');
    const newCategoryContainer = document.getElementById('newCategoryContainer');
    const newCategoryInput = document.getElementById('newCategoryInput');
    const newSubCategoryContainer = document.getElementById('newSubCategoryContainer');
    const newSubCategoryInput = document.getElementById('newSubCategoryInput');

    // Parse data attributes
    let categories = [];
    let subCategories = [];

    try {
        const categoriesJson = categoriesDataElement.getAttribute('data-categories');
        if (categoriesJson) {
            categories = JSON.parse(categoriesJson);
            console.log("Categories:", categories);
        }

        const subCategoriesJson = subCategoriesDataElement.getAttribute('data-subcategories');
        if (subCategoriesJson) {
            subCategories = JSON.parse(subCategoriesJson);
            console.log("SubCategories:", subCategories);
        }
    } catch (error) {
        console.error("Error parsing JSON:", error);
    }

    // Add "Add New" option to category dropdown
    const addNewCategoryOption = new Option("+ Add New Category", "new");
    categorySelect.add(addNewCategoryOption);

    // Add "Add New" option to sub-category dropdown initially
    const addNewSubCategoryOption = new Option("+ Add New Sub-Category", "new");
    subCategorySelect.add(addNewSubCategoryOption);


    // Populate category dropdown
    categories.forEach(category => {
        const option = new Option(category.name, category.id);
        // Insert before the last option (Add New)
        categorySelect.add(option, categorySelect.options.length - 1);
    });

    // Handle category selection change
    categorySelect.addEventListener('change', function() {
        const selectedValue = this.value;

        // Show/hide new category input
        if (selectedValue === "new") {
            newCategoryContainer.style.display = "block";
            newCategoryInput.focus();
        } else {
            newCategoryContainer.style.display = "none";
        }

        // Clear and update subcategory dropdown
        subCategorySelect.innerHTML = '<option value="">-- Select Sub-Category (Optional) --</option>';

        if (selectedValue && selectedValue !== "new") {
            const categoryId = parseInt(selectedValue, 10);

            // Filter subcategories for this category
            const filteredSubcategories = subCategories.filter(sub => {
                // Convert both to numbers for reliable comparison
                return Number(sub.categoryId) === categoryId;
            });

            console.log("Selected category ID:", categoryId);
            console.log("Filtered subcategories:", filteredSubcategories);

            // Add filtered subcategories to dropdown
            filteredSubcategories.forEach(subCategory => {
                const option = new Option(subCategory.name, subCategory.id);
                subCategorySelect.add(option);
            });

            // Add the "Add New" option back to sub-categories
            subCategorySelect.add(addNewSubCategoryOption.cloneNode(true)); // Clone to avoid issues if it was selected
        }

        // Always hide new sub-category input when category changes
        newSubCategoryContainer.style.display = "none";
        newSubCategoryInput.value = ''; // Clear the input
    });

    // Handle sub-category selection change
    subCategorySelect.addEventListener('change', function() {
        if (this.value === "new") {
            newSubCategoryContainer.style.display = "block";
            newSubCategoryInput.focus();
        } else {
            newSubCategoryContainer.style.display = "none";
            newSubCategoryInput.value = ''; // Clear the input if another option is chosen
        }
    });


    // Form submission validation
    document.querySelector('form').addEventListener('submit', function(e) {
        const selectedCategory = categorySelect.value;

        // Validate category
        if (selectedCategory === "") {
            alert("Please select a category");
            e.preventDefault();
            return;
        }

        // If "Add New Category" is selected, validate the input
        if (selectedCategory === "new" && !newCategoryInput.value.trim()) {
            alert("Please enter a name for the new category");
            newCategoryInput.focus();
            e.preventDefault();
            return;
        }

        // If "Add New Sub-Category" is selected, validate the input
        const selectedSubCategory = subCategorySelect.value;
        if (selectedSubCategory === "new" && !newSubCategoryInput.value.trim()) {
            alert("Please enter a name for the new sub-category");
            newSubCategoryInput.focus();
            e.preventDefault();
            return;
        }
    });
});
