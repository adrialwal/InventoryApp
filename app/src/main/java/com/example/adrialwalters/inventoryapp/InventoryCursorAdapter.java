package com.example.adrialwalters.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.adrialwalters.inventoryapp.data.InventoryContract.ProductEntry;

public class InventoryCursorAdapter extends CursorAdapter{

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     * @param context the content
     * @param cursor the cursor from which ti get the data
     */
    public InventoryCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor,0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent,false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find the views to inflate
        TextView brandTextView = (TextView) view.findViewById(R.id.text_item_brand);
        TextView modelTextView = (TextView) view.findViewById(R.id.text_item_model);
        TextView priceTextView = (TextView) view.findViewById(R.id.text_item_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.text_item_quantity);
        Button saleButton = (Button) view.findViewById(R.id.button_list_sale);
        Button restockButton = (Button) view.findViewById(R.id.button_list_restock);

        // Get the data from cursor
        String productBrand = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_BRAND));
        String productModel = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_MODEL));
        String productPrice = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE));
        String productQuantity = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));

        // Set data to view
        brandTextView.setText(productBrand);
        modelTextView.setText(productModel);
        priceTextView.setText(String.format("%s%s",
                context.getResources().getText(R.string.dollar_sign), productPrice));
        quantityTextView.setText(String.format("%s: %s",
                context.getResources().getText(R.string.stock, productQuantity),quantityTextView));

        // Variables needed to decrease inventory
        final int rowID = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));
        final Uri uriToUpdate = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, rowID);
        final int currentQuantity = Integer.parseInt(productQuantity);

        // When the sale button is pressed the quantity will decrease by 1
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentQuantity > 0) {
                    int decreaseQuantity = currentQuantity - 1;
                    ContentValues updateValue = new ContentValues();
                    updateValue.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, decreaseQuantity);
                    context.getContentResolver().update(uriToUpdate, updateValue, null, null);
                    quantityTextView.setText(String.format("%s: %s",
                            context.getResources().getText(R.string.stock), decreaseQuantity));
                }
            }
        });
    }
}
