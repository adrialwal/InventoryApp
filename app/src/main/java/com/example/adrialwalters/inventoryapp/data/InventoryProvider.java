package com.example.adrialwalters.inventoryapp.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.adrialwalters.inventoryapp.data.InventoryContract.ProductEntry;

/**
 * {@link ContentProvider} for Inventory app.
 */
public class InventoryProvider extends ContentProvider {

    /** Tag for the log message */
    private static final String TAG = InventoryProvider.class.getSimpleName();

    /** URI mather code for the content URI for the products table */
    private static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single products table */
    private static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static{
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.adrialwalters.inventoryapp/products"
        // will map to the integer code {@link #PRODUCTS}. This URI is used to provide access to
        // MULTIPLE rows of the products table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);

        // The content URI of the form "content://com.example.adrialwalters.inventoryapp/products/#"
        // will map to the integer code {@link #PRODUCT_ID}. This URI is used to provide access to ONE
        // single row of the products table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // for example, "content://com.example.adrialwalters.inventoryapp/products/3" matches, but
        // "content://com.example.adrialwalters.inventoryapp/products" (without a number at the end)
        // doesn't match.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /** Database helper object */
    private InventoryDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // The cursor to be returned
        Cursor cursor;

        // Find if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return  ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            // Insert operation will always be implemented on the products table as a whole
            // not on a specific product
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion not supported for " + uri);
        }
    }

    /**
     * Helper method to insert new data into the provider with the given ContentValues.
     */
    private Uri insertProduct(Uri uri, ContentValues contentValues) {
        // Check that the name is not null
        String productBrand = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_BRAND);
        if (productBrand == null) {
            throw new IllegalArgumentException("Products brand required");
        }

        String productModel = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_MODEL);
        if (productModel == null) {
            throw new IllegalArgumentException("Product model required");
        }

        // Check if price < 0
        String productPriceString = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_PRICE);
        int productPrice = Integer.parseInt(productPriceString);
        if (productPrice < 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        // Check if quantity < 0
        String productQuantityString = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int productQuantity = Integer.parseInt(productQuantityString);
        if (productQuantity < 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // Check that the name is not null
        String supplierName = contentValues.getAsString(ProductEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Supplier name required");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Row ID of the newly inserted row, or -1 if an error occurred
        long rowId = database.insert(ProductEntry.TABLE_NAME, null, contentValues);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (rowId == -1) {
            Log.e(TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that data has changed for content URI
        // Passing null, by default will notify CursorAdapter object of changes
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(ProductEntry.CONTENT_URI, rowId);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (sUriMatcher.match(uri)) {
            // Delete all entries that match the selection and selection args
            case PRODUCTS:
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            // Delete a specific entry in the products table
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Delete not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            // Notify all listeners that data has changed for content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues,
                      String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            // Update all entries that match the selection and selection args
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            // Update a specific entry in the products table
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update not supported for " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues contentValues,
                              String selection, String[] selectionArgs) {
        // If the {@link ProductEntry#COLUMN_PRODUCT_Brand} key is present,
        // check that the name value is not null.
        if (contentValues.size() == 0) {
            return 0;
        }

        // If products brand exists, check if model == null
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_BRAND)) {
            String productBrand = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_BRAND);
            if (productBrand == null) {
                throw new IllegalArgumentException("Products brand required");
            }
        }

        // If product model exists, check if model == null
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_MODEL)) {
            String productModel = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_MODEL);
            if (productModel == null) {
                throw new IllegalArgumentException("Product model required");
            }
        }

        // If product price exists, check if price < 0
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            String productPriceString = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_PRICE);
            int productPrice = Integer.parseInt(productPriceString);
            if (productPrice < 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
        }

        // If product quantity exists, check if quantity < 0
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            String productQuantityString = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int productQuantity = Integer.parseInt(productQuantityString);
            if (productQuantity < 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
        }

        // If supplier name exists, check if name == null
        if (contentValues.containsKey(ProductEntry.COLUMN_SUPPLIER_NAME)) {
            String supplierName = contentValues.getAsString(ProductEntry.COLUMN_SUPPLIER_NAME);
            if (supplierName == null) {
                throw new IllegalArgumentException("Supplier name required");
            }
        }

        // Get db reference, then update db and get number of rows affected
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        if (rowsUpdated != 0) {
            // Notify all listeners that data has changed for content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }
}
