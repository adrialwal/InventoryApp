package com.example.adrialwalters.inventoryapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.adrialwalters.inventoryapp.data.InventoryContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the product data loader */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /** Pick image request code */
    private static final int PICK_IMAGE_REQUEST = 1;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    /** Content URI for the existing image (null if it's a new image) */
    private Uri mCurrentImageUri;

    /** EditText field to enter the brand */
    private EditText mBrandEditText;

    /** EditText field to enter the model */
    private EditText mModelEditText;

    /** EditText field to enter the price */
    private EditText mPriceEditText;

    /** EditText field to enter the quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter the supplier name */
    private EditText mSupplierEditText;

    /** EditText field to enter the suppliers email */
    private EditText mEmailEditText;

    /** Button field for sale */
    private Button mSaleButton;

    /** Button field for restock */
    private Button mRestockButton;

    /** ImageView field for product image */
    private ImageView mImageView;

    /** Boolean flag that keeps track of whether the product has been edited (true) or not (false) */
    private boolean mProductHasChanged = false;

    /** Catch errors when uploading image */
    private static final String TAG = EditorActivity.class.getSimpleName();


    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new producy or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain product content URI, then we know that we are
        // creating a new product.
        if(mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mBrandEditText = (EditText) findViewById(R.id.edit_editor_field_brand);
        mModelEditText = (EditText) findViewById(R.id.edit_editor_field_model);
        mPriceEditText = (EditText) findViewById(R.id.edit_editor_field_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_editor_field_quantity);
        mSupplierEditText = (EditText) findViewById(R.id.edit_editor_field_supplier);
        mEmailEditText = (EditText) findViewById(R.id.edit_editor_field_email);
        mSaleButton = (Button) findViewById(R.id.sale_button);
        mRestockButton = (Button) findViewById(R.id.restock_button);
        mImageView = (ImageView) findViewById(R.id.product_image);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.image_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector();
            }
        });

        // Prepare the loader.  Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mBrandEditText.setOnTouchListener(mTouchListener);
        mModelEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mEmailEditText.setOnTouchListener(mTouchListener);

        // Setup OnClickListeners for Sale button to decrement stock by 1 upon click
        mSaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentQuantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());

                // Decrement only if quantity > 0
                if (currentQuantity > 0) {
                    int decreasedQuantity = currentQuantity - 1;
                    mQuantityEditText.setText(String.format("%s", decreasedQuantity));
                }
                // If user clicks on this button, an edit has been made
                mProductHasChanged = true;
            }
        });

        // Setup OnClickListeners for Restock button to increment stock by 1 upon click
        mRestockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentQuantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());

                int increasedQuantity = currentQuantity + 1;
                mQuantityEditText.setText(String.format("%s", increasedQuantity));

                // If user clicks on this button, an edit has been made
                mProductHasChanged = true;
            }
        });
    }

    /**
     * Get user input from editor and save product into database.
     */
    private void saveProduct() {
        // Get String values from details editor field
        String productBrand = mBrandEditText.getText().toString().trim();
        String productModel = mModelEditText.getText().toString().trim();
        String productPrice = mPriceEditText.getText().toString().trim();
        String productQuantity = mQuantityEditText.getText().toString().trim();
        String productSupplier = mSupplierEditText.getText().toString().trim();
        String productEmail = mEmailEditText.getText().toString().trim();


        // If all fields are empty or if required fields (Model & Supplier) not provided
        // then exit activity w/o saving
        if (mCurrentImageUri == null &&
                TextUtils.isEmpty(productModel) && TextUtils.isEmpty(productSupplier) &&
                TextUtils.isEmpty(productPrice) && TextUtils.isEmpty(productQuantity)
                && TextUtils.isEmpty(productEmail)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        int price = 0;
        int quantity = 0;

        if (!TextUtils.isEmpty(productBrand)) {
            values.put(ProductEntry.COLUMN_PRODUCT_BRAND, productBrand);
        }
        if (!TextUtils.isEmpty(productModel)) {
            values.put(ProductEntry.COLUMN_PRODUCT_MODEL, productModel);
        }
        if (!TextUtils.isEmpty(productPrice)) {
            price = Integer.parseInt(productPrice);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (!TextUtils.isEmpty(productQuantity)) {
            quantity = Integer.parseInt(productQuantity);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        if (!TextUtils.isEmpty(productSupplier)) {
            values.put(ProductEntry.COLUMN_SUPPLIER_NAME, productSupplier);
        }
        if (!TextUtils.isEmpty(productEmail)) {
            values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, productEmail);
        }
        if (mCurrentImageUri != null) {
            values.put(ProductEntry.COLUMN_STRING_IMAGE_URI, mCurrentImageUri.toString());
        }
        if (mCurrentProductUri == null) {
            // If URI == null, saving a new product
            // Insert product into db
            Uri newuri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            if (newuri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, R.string.toast_insert_product_failed,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Else insertion was successful
                Toast.makeText(this, R.string.toast_insert_product_successful,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Else updating an existing product
            int rowUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowUpdated == 0) {
                // If no product was updated, then an error occurred
                Toast.makeText(this, R.string.editor_update_product_failed,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Else product update successful
                Toast.makeText(this, R.string.editor_update_product_successful,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "delete" and "order product" menu items
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        } if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_order_product);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Order Product" menu option
            case R.id.action_order_product:
                // Opens email app
                String[] arrayEmailTo = new String[]{mEmailEditText.getText().toString().trim()};
                String subject = getResources().getString(R.string.order_summary);
                createOrderEmail(arrayEmailTo, subject, createOrderSummary());
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

            // Otherwise if there are unsaved changes, setup a dialog to warn the user.
            // Create a click listener to handle the user confirming that
            // changes should be discarded.
            DialogInterface.OnClickListener discardButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // User clicked "Discard" button, navigate to parent activity.
                            NavUtils.navigateUpFromSameTask(EditorActivity.this);
                        }
                    };

            // Show a dialog that notifies the user they have unsaved changes
            showUnsavedChangesDialog(discardButtonClickListener);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            // If data hasn't changed, continue with handling back button press
            super.onBackPressed();

            // Otherwise if there are unsaved changes, setup a dialog to warn the user.
            // Create a click listener to handle the user confirming that changes should be discarded.
            DialogInterface.OnClickListener discardButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // User clicked "Discard" button, close the current activity.
                            finish();
                        }
                    };

            // Show dialog that there are unsaved changes
            showUnsavedChangesDialog(discardButtonClickListener);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle args) {
        // Check that mCurrentProductUri is not null
        if (mCurrentProductUri == null) {
            return null;
        }
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_BRAND,
                ProductEntry.COLUMN_PRODUCT_MODEL,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_SUPPLIER_NAME,
                ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_STRING_IMAGE_URI};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Exit early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int brandColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_BRAND);
            int modelColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MODEL);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_NAME);
            int emailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_STRING_IMAGE_URI);


            // Get String using Cursor
            String brand = cursor.getString(brandColumnIndex);
            String model = cursor.getString(modelColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            String quantity = cursor.getString(quantityColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            String email = cursor.getString(emailColumnIndex);
            String image = cursor.getString(imageColumnIndex);

            // Set text and image
            mBrandEditText.setText(brand);
            mModelEditText.setText(model);
            mPriceEditText.setText(price);
            mQuantityEditText.setText(quantity);
            mSupplierEditText.setText(supplier);
            mEmailEditText.setText(email);
            if (!TextUtils.isEmpty(image)) {
                Uri imageUri = Uri.parse(image);
                mImageView.setImageBitmap(getBitmapFromUri(imageUri));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mBrandEditText.setText("");
        mModelEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierEditText.setText("");
        mEmailEditText.setText("");

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mCurrentImageUri = resultData.getData();
                mImageView.setImageBitmap(getBitmapFromUri(mCurrentImageUri));
                mProductHasChanged = true;
            }
        }
    }

    /**
     * Create email to order more products
     */
    private void createOrderEmail(String[] arrayEmailTo, String subject, String orderSummary) {
        // ACTION_SENDTO = compose an email w no attachments
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        //Only email apps should handle this
        intent.setData(Uri.parse("mailto:"));
        // String array of "To" recipients
        intent.putExtra(Intent.EXTRA_EMAIL, arrayEmailTo);
        // String w the email subject
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        // String w the email body
        intent.putExtra(Intent.EXTRA_TEXT, orderSummary);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * Create order summary
     */
    private String createOrderSummary(){
        String supplierName = mSupplierEditText.getText().toString().trim();
        String productModel = mModelEditText.getText().toString().trim();
        String productPrice = mPriceEditText.getText().toString().trim();

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s: %s", getResources().getString(R.string.supplier_category),
                supplierName));
        sb.append("\n");
        sb.append(String.format("%s: %s", getResources().getString(R.string.model_category),
                productModel));
        sb.append("\n");
        sb.append(String.format("%s: %s", getResources().getString(R.string.price_category),
                productPrice));

        return sb.toString();
    }

    /**
     * Open image selector activity
     */
    private void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    /**
     *     Gets a bitmap from the given URI
     */
    private Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            //bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg).
                setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked the "Delete" button
                        deleteProduct();
                    }
                }).
                setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        // User clicked the "Cancel" button, so dismiss the dialog
                        // and continue editing
                        if (dialogInterface != null) {
                            dialogInterface.dismiss();
                        }
                    }
                });
        // Create and show the AlertDialog
        builder.create().show();
    }

    /**
     * Perform the deletion of a product in the database.
     */
    private void deleteProduct() {
        int rowDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

        if (rowDeleted == 0) {
            Toast.makeText(this, R.string.editor_delete_product_failed,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_delete_product_successful,
                    Toast.LENGTH_SHORT).show();
        }
        // Close the activity
        finish();
    }
}