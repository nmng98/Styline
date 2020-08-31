package com.example.virtualcloset;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.MenuItem;
import android.view.View;

import android.widget.ImageView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;

import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import in.goodiebag.carouselpicker.CarouselPicker;


public class MainActivity extends AppCompatActivity {
    // DEFINED FOR CAROUSEL
    private String  UserUID;
    private Bitmap bm;
    private List<CarouselPicker.PickerItem> topItems = new ArrayList<>();
    private List<CarouselPicker.PickerItem> bottomItems = new ArrayList<>();
    private StorageReference storageReference, pathTopReference, pathBottomReference;
    private FirebaseStorage storage;
    private CarouselPicker topCarousel, bottomCarousel;
    private final long ONE_MEGABYTE = 1024 * 1024;
    //DEFINED FOR CAMERA
    private ImageView cameraActvivityButton;
    //DEFINED FOR MENU DRAWER
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;

    private List<String> shirtNames;
    private List<String> pantsNames;
    private ArrayList<String> starred;
    public ImageView starIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraActvivityButton = this.findViewById(R.id.startCameraActivityButton);
        topCarousel = this.findViewById(R.id.carouselTop);
        bottomCarousel = this.findViewById(R.id.carouselBottom);


        //STAR FUNCTION
        starred = new ArrayList<>();
        //set up database
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        String user = FirebaseAuth.getInstance().getUid();
        final DatabaseReference db = firebaseDatabase.getReference("users/" + user +
                "/clothes/");
        db.child("Starred");

        //create new list of starred, else download from database
            db.child("Starred").addListenerForSingleValueEvent(new ValueEventListener(){
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {
                    };
                    ArrayList<String> temp = dataSnapshot.getValue(t);
                    starred.clear();

                    if(temp!= null)
                        starred = temp;

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        starIcon = findViewById(R.id.star);
        ImageView starButton = findViewById(R.id.star);
        shirtNames = new ArrayList<>();
        pantsNames = new ArrayList<>();

        //click listener for star button
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get current shirt and pants names
                String shirt = shirtNames.get(topCarousel.getCurrentItem());
                String pants = pantsNames.get(bottomCarousel.getCurrentItem());


                //download from database when new info is added
                db.child("Starred").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {
                        };
                        ArrayList<String> temp = dataSnapshot.getValue(t);
                        starred.clear();

                        if(temp!=null)
                            starred = temp;

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //change image color when clicked
                starIcon.setImageDrawable(getResources().getDrawable(R.drawable.star_icon_filled));

                //upload to database
                String starPair = shirt + "," + pants;
                starred.add(starPair);
                db.child("Starred").setValue(starred);

                String str = shirtNames.get(topCarousel.getCurrentItem());
            }
        });

        // FIREBASE SETUP
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        UserUID = FirebaseAuth.getInstance().getUid();

        // MENU SETUP
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //change later to hamburger (three lines icon)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawer = findViewById(R.id.drawer_layout);
        nvDrawer = findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);



        // ADD CAROUSEL
        pathTopReference = storageReference.child("users/" + UserUID +
                "/clothes/" + "top" + "/");
        pathBottomReference = storageReference.child("users/" + UserUID +
                "/clothes/" + "bottom" + "/");
        addCarousel(topItems, shirtNames, pathTopReference, topCarousel);
        addCarousel(bottomItems, pantsNames, pathBottomReference, bottomCarousel);


        // CAMERA ACTIVITY
        cameraActvivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        topCarousel.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


            }

            @Override
            public void onPageSelected(int position) {
                try {
                    String current = shirtNames.get(position) + "," + pantsNames.get(bottomCarousel.getCurrentItem());
                    if (starred.contains(current)) {
                        starIcon.setImageDrawable(getResources().getDrawable(R.drawable.star_icon_filled));

                    } else {
                        starIcon.setImageDrawable(getResources().getDrawable(R.drawable.star));

                    }
                }catch(Exception e){

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        bottomCarousel.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


            }

            @Override
            public void onPageSelected(int position) {
                try {
                    String current = shirtNames.get(topCarousel.getCurrentItem()) + "," + pantsNames.get(position);
                    if (starred.contains(current)) {
                        starIcon.setImageDrawable(getResources().getDrawable(R.drawable.star_icon_filled));
                    } else {
                        starIcon.setImageDrawable(getResources().getDrawable(R.drawable.star));
                    }
                }catch(Exception e){

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    // ADDS TOP CLOTHING FROM USER STORAGE
    // AND DISPLAYS IN FIRST CAROUSEL
    public void addCarousel(final List<CarouselPicker.PickerItem> items, final List<String> itemNames, StorageReference ref, final CarouselPicker carousel) {
        //final List<String> names = new ArrayList<>();
        ref.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for(StorageReference filteref: listResult.getItems()) {
                    itemNames.add(filteref.getName());
                    filteref.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            bm = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);
                            items.add(new CarouselPicker.BitmapItem(bm));
                            CarouselPicker.CarouselViewAdapter adapter =

                                    new CarouselPicker.CarouselViewAdapter(MainActivity.this,
                                            items, itemNames,0);
                            carousel.setAdapter(adapter);

                            carousel.setCurrentItem(adapter.getCount()/2);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "FAILED DATABASE SYNC",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "NO CLOTHING AVAILABLE TO DISPLAY",
                        Toast.LENGTH_LONG).show();
            }
        });

    }






    public void showDialog(View theView){

    }

    // MENU CONFIGURATION
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
//        if (item.getItemId() == android.R.id.home) {
//            mDrawer.openDrawer(GravityCompat.START);
//            return true;
//        }
        if(item.getItemId() == R.id.nav_first_fragment){
            Toast.makeText(this, "FIRST FRAGMENT1", Toast.LENGTH_SHORT).show();
            selectDrawerItem(item);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NotNull MenuItem menuItem) {
                        //selectDrawerItem(menuItem);
                        if(menuItem.getItemId() == R.id.nav_first_fragment){
                            //Toast.makeText(MainActivity.this, "CLOSET", Toast.LENGTH_SHORT).show();
                            selectDrawerItem(menuItem);
                            return true;
                        }else if(menuItem.getItemId() == R.id.nav_second_fragment){
                            selectDrawerItem(menuItem);
                            return true;
                        }else if(menuItem.getItemId() == R.id.nav_third_fragment){
                            selectDrawerItem(menuItem);
                            return true;
                        }else
                            return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        fragmentClass = TestFragment1.class;
        TextView text;
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View mView = LayoutInflater.from(this).inflate(R.layout.menu_dialog, null);
        final AlertDialog alertDialog = alert.create();

        switch (menuItem.getItemId()) {
            case R.id.nav_first_fragment:
                fragmentClass = TestFragment1.class;

                text = mView.findViewById(R.id.text_popup);
                text.setText("Navigating to your closet...");
                alert.setView(mView);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                alertDialog.setCanceledOnTouchOutside(true);
                alert.show();
                break;

            case R.id.nav_second_fragment:

                fragmentClass = TestFragment1.class;

                text = mView.findViewById(R.id.text_popup);
                text.setText("Navigating to search...");
                alert.setView(mView);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                alertDialog.setCanceledOnTouchOutside(true);
                alert.show();
                break;

            case R.id.nav_third_fragment:
                fragmentClass = TestFragment1.class;

                text = mView.findViewById(R.id.text_popup);
                text.setText("Navigating to settings...");
                alert.setView(mView);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                alertDialog.setCanceledOnTouchOutside(true);
                alert.show();
                break;

            default:
                fragmentClass = TestFragment1.class;
                text = mView.findViewById(R.id.text_popup);
                text.setText("Navigating ...");
                alert.setView(mView);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                alertDialog.setCanceledOnTouchOutside(true);
                alert.show();
                break;

        }


//        try {
//            fragment = (Fragment) fragmentClass.newInstance();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//
//        // Insert the fragment by replacing any existing fragment
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    public void onDrawerClicked(View v){
        mDrawer.openDrawer(GravityCompat.START);
    }


}
