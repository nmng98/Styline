package in.goodiebag.carouselpicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by pavan on 25/04/17.
 */

public class CarouselPicker extends ViewPager {
    private int itemsVisible = 3;
    private float divisor;
    public Context mainContext;


    public CarouselPicker(Context context) {
        this(context, null);
    }

    public CarouselPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CarouselPicker);
            itemsVisible = array.getInteger(R.styleable.CarouselPicker_items_visible, itemsVisible);
            switch (itemsVisible) {
                case 3:
                    TypedValue threeValue = new TypedValue();
                    getResources().getValue(R.dimen.three_items, threeValue, true);
                    divisor = threeValue.getFloat();
                    break;
                case 5:
                    TypedValue fiveValue = new TypedValue();
                    getResources().getValue(R.dimen.five_items, fiveValue, true);
                    divisor = fiveValue.getFloat();
                    break;
                case 7:
                    TypedValue sevenValue = new TypedValue();
                    getResources().getValue(R.dimen.seven_items, sevenValue, true);
                    divisor = sevenValue.getFloat();
                    break;
                default:
                    divisor = 3;
                    break;
            }
            array.recycle();
        }
    }

    private void init() {
        this.setPageTransformer(false, (PageTransformer) new CustomPageTransformer(getContext()));
        this.setClipChildren(false);
        this.setFadingEdgeLength(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();

        // Add 250 pixels for padding between images
        setPageMargin((int) (-w / divisor) + 250);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);
        this.setOffscreenPageLimit(adapter.getCount());
    }

    public static class CarouselViewAdapter extends PagerAdapter {

        List<PickerItem> items = new ArrayList<>();
        public List<String> names = new ArrayList<>();
        Context context;
        ImageView imageView;
        int drawable;
        int textColor = 0;

        public CarouselViewAdapter(Context context, List<PickerItem> items, List<String> names, int drawable) {
            this.context = context;
            this.items = items;
            this.names = names;
            this.imageView = imageView;
            if (this.drawable == 0) {
                this.drawable = R.layout.page;
            }
        }


        @Override
        public int getCount() {
            return items.size();
        }

        

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            View view = LayoutInflater.from(context).inflate(this.drawable, null);
            ImageView iv = (ImageView) view.findViewById(R.id.iv);
            PickerItem pickerItem = items.get(position);
            if (pickerItem.hasDrawable()) {
                iv.setVisibility(VISIBLE);
                iv.setImageResource(pickerItem.getDrawable());
            }
            if(pickerItem != null) {
                iv.setVisibility(VISIBLE);
                iv.setImageBitmap(pickerItem.getBitmap());
            }
            else {
                if (pickerItem.getText() != null) {
                    iv.setVisibility(GONE);
                }
            }


            iv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                    String user = FirebaseAuth.getInstance().getUid();
                    final DatabaseReference db = firebaseDatabase.getReference("users/" + user +
                            "/clothes/");
                    final HashMap<String, String> tagged = new HashMap<>();
                    // debug
                    final AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    View mView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
                    final TextView clothing_info = mView.findViewById(R.id.clothing_info);
                    final EditText brand_popup = mView.findViewById(R.id.brand_popup);
                    final EditText type_popup = mView.findViewById(R.id.type_popup);
                    final EditText tags_popup = mView.findViewById(R.id.tags_popup);
                    Button save_button = mView.findViewById(R.id.save_button);
                    alert.setView(mView);
                    final AlertDialog alertDialog = alert.create();
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    alertDialog.setCanceledOnTouchOutside(true);
                    save_button.setOnClickListener( new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            db.child(names.get(position)).child("title").
                            setValue(brand_popup.getText().toString());
                            db.child(names.get(position)).child("bought").
                                    setValue(type_popup.getText().toString());
                            StringTokenizer tokenizer = new StringTokenizer(tags_popup.getText().toString(), "\\s*,\\s*");
                            int i = 0;
                            while(tokenizer.hasMoreTokens()) {
                                tagged.put(String.valueOf(i), tokenizer.nextToken().trim());
                                i++;
                            }
                            db.child(names.get(position)).child("tags").setValue(tagged);
                        }
                    });
                    alert.show();
                }
            });

            view.setTag(position);
            container.addView(view);
            return view;
        }

        public int getTextColor() {
            return textColor;
        }

        public void setTextColor(@ColorInt int textColor) {
            this.textColor = textColor;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return (view == object);
        }

        private int dpToPx(int dp) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        }
    }

    /**
     * An interface which should be implemented by all the Item classes.
     * The picker only accepts items in the form of PickerItem.
     */
    public interface PickerItem {
        String getText();

        Bitmap getBitmap();

        @DrawableRes
        int getDrawable();

        boolean hasDrawable();
    }

    /**
     * A PickerItem which supports text.
     */
    public static class TextItem implements PickerItem {
        private String text;
        private int textSize;

        public TextItem(String text, int textSize) {
            this.text = text;
            this.textSize = textSize;
        }

        public String getText() {
            return text;
        }

        @Override
        public int getDrawable() {
            return 0;
        }

        @Override
        public Bitmap getBitmap() {
            return null;
        }

        @Override
        public boolean hasDrawable() {
            return false;
        }

        public int getTextSize() {
            return textSize;
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
        }
    }

    /**
     * A PickerItem which supports drawables.
     */
    public static class BitmapItem implements PickerItem {
        private Bitmap bit;

        public BitmapItem(Bitmap bit) {
            this.bit = bit;
        }

        @Override
        public String getText() {
            return null;
        }

        public Bitmap getBitmap() {
            return bit;
        }


        @DrawableRes
        public int getDrawable() {
            return 0;
        }

        @Override
        public boolean hasDrawable() {
            return true;
        }
    }
    public static class DrawableItem implements PickerItem {
        @DrawableRes
        private int drawable;

        public DrawableItem(@DrawableRes int drawable) {
            this.drawable = drawable;
        }

        @Override
        public String getText() {
            return null;
        }

        @DrawableRes
        public int getDrawable() {
            return drawable;
        }

        @Override
        public boolean hasDrawable() {
            return true;
        }

        public Bitmap getBitmap() {
            return null;
        }
    }
}
