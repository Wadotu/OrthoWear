package wadotu.orthodoxy.orthowear.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import wadotu.orthodoxy.orthowear.R;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    private final List<Integer> images;

    public ImageAdapter(List<Integer> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.imageView.setImageResource(images.get(position));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        VH(View v) {
            super(v);
            imageView = v.findViewById(R.id.imageView);
        }
    }
}