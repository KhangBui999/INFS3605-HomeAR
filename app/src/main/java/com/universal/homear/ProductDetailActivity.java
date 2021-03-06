package com.universal.homear;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private String TAG = "com.universal.homear.ProductDetailActivity";

    private TextView mName, mPrice, mDetail, mColour, mDimensions, mWeight;
    private Button viewAR, cartBtn;
    private ImageView mImage, backBtn;
    private Furniture furniture;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        mAuth = FirebaseAuth.getInstance();

        mName = findViewById(R.id.productName);
        mPrice = findViewById(R.id.productPrice);
        mDetail = findViewById(R.id.productDesc);
        mColour = findViewById(R.id.productColour);
        mDimensions = findViewById(R.id.productDim);
        mWeight = findViewById(R.id.productWeight);
        mImage = findViewById(R.id.imageView2);
        cartBtn = findViewById(R.id.contactButton);
        backBtn = findViewById(R.id.iv_backBtn);
        viewAR = findViewById(R.id.viewARButton);

        String furnitureId = getIntent().getStringExtra("PRODUCT_ID");

        getFurnitureData(furnitureId);
        renderButtons();

    }

    private void getFurnitureData(String furnitureId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("Furniture").document(furnitureId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> furnitureMap = document.getData();
                        Long ucPrice = (Long) furnitureMap.get("price");
                        Long ucStock = (Long) furnitureMap.get("stock");

                        String id = document.getId();
                        String name = furnitureMap.get("name").toString();
                        int price = ucPrice.intValue();
                        int stock = ucStock.intValue();
                        String detail = furnitureMap.get("detail").toString();
                        String colour = furnitureMap.get("colour").toString();
                        String dimensions = furnitureMap.get("dimensions").toString();
                        String weight = furnitureMap.get("weight").toString();

                        furniture = new Furniture(id, name, price, stock, detail, colour, dimensions, weight);

                        renderFurnitureDetail();
                    }
                }
            }
        });
    }

    private void renderFurnitureDetail() {
        mName.setText(furniture.getName());
        mPrice.setText(Integer.toString(furniture.getPrice()));
        loadImage();
        mDetail.setText(furniture.getDetail());
        mColour.setText("Colour: " + furniture.getColour());
        mDimensions.setText("Dimensions (l, w, h): " + furniture.getDimensions());
        mWeight.setText("Weight: " + furniture.getWeight());
    }

    private void loadImage() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference modelRef = storage.getReference().child(furniture.getId() + ".jpeg");
        Glide.with(this)
                .load(modelRef)
                .into(mImage);
    }

    private void renderButtons() {
        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemToCart();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductDetailActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        viewAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductDetailActivity.this, ArViewerActivity.class);
                intent.putExtra("objectFileId", furniture.getId());
                startActivity(intent);
            }
        });
    }

    private void addItemToCart() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        final boolean[] change = {false};

        DocumentReference cartRef = db.collection("ShoppingCart").document(user.getUid());
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(cartRef);
                ArrayList<String> cartList = (ArrayList<String>) snapshot.get("cartItems");
                if(!cartList.contains(furniture.getId())) {
                    cartList.add(furniture.getId());
                    transaction.update(cartRef, "cartItems", cartList);
                    change[0] = true;
                }
                return null;
            }
        })
        .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if(change[0]) {
                    Toast.makeText(ProductDetailActivity.this, "Item Added to Cart!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(ProductDetailActivity.this, "Item Already in Cart!", Toast.LENGTH_SHORT).show();
                }
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProductDetailActivity.this, "Transaction Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}