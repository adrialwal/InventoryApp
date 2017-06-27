package com.example.adrialwalters.inventoryapp.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // The cursor to be returned
        Cursor cursor;

        // Find if the URI matcher can match the URI to a specific code
        switch (sUriMatcher.match(uri)) {
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
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
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
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        switch (sUriMatcher.match(uri)) {
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
        /* Check values before inserting */
        // Check if model == null
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

        // Check if supplier name == null
        String supplierName = contentValues.getAsString(ProductEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Supplier name required");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Row ID of the newly inserted row, or -1 if an error occurred
        long rowId = database.insert(ProductEntry.TABLE_NAME, null, contentValues);

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
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

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
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
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

    private int updateProduct(@NonNull Uri uri, @Nullable ContentValues contentValues,
                            @Nullable String selection, @Nullable String[] selectionArgs) {
        /* Check values before updating */
        // If contentValues is empty, return 0
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
