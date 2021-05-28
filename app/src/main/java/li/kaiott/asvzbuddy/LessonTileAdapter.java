package li.kaiott.asvzbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class LessonTileAdapter extends RecyclerView.Adapter<LessonTileAdapter.LessonViewHolder> {

    ArrayList<Lesson> lessons;
    private final Context context;

    public LessonTileAdapter(Context ct, ArrayList<Lesson> lessons) {
        context = ct;
        this.lessons = lessons;
        Lesson.selectedCount = 0;
    }

    @NonNull
    @Override
    public LessonTileAdapter.LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.lesson_tile, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonTileAdapter.LessonViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);
        holder.titleText.setText(lesson.getTitle());
        holder.idText.setText(""+lesson.getId());
        holder.statusText.setText(Lesson.statusDescriptors[lesson.getStatus()]);
        holder.dateText.setText(lesson.getStarts().format(DateTimeFormatter.ofPattern("E dd.MM.yyyy")));
        holder.timeText.setText(lesson.getStarts().format(DateTimeFormatter.ofPattern("kk:mm")));

        holder.innerConstraint.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!lesson.selected) {
                    lesson.selected = true;
                    Lesson.selectedCount++;
                    holder.selectedImage.setVisibility(View.VISIBLE);
                    ((MainActivity) context).updateMenu();
                }
                return true;
            }
        });
        holder.innerConstraint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lesson.selected) {
                    lesson.selected = false;
                    Lesson.selectedCount--;
                    holder.selectedImage.setVisibility(View.GONE);
                    ((MainActivity) context).updateMenu();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, idText, statusText, dateText, timeText;
        ImageView selectedImage;
        ConstraintLayout innerConstraint;
        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_lesson_title);
            idText = itemView.findViewById(R.id.text_lesson_id);
            statusText = itemView.findViewById(R.id.text_lesson_status);
            dateText = itemView.findViewById(R.id.text_lesson_date);
            timeText = itemView.findViewById(R.id.text_lesson_time);
            selectedImage = itemView.findViewById(R.id.image_lesson_selected);
            innerConstraint = itemView.findViewById(R.id.constraint_lesson_inner);
        }
    }
}
