package com.mobileapplication.streetassist.ui.resident.News;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.RegisterActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView rvAnnouncements;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AnnouncementAdapter adapter;
    private final List<Announcement> announcements = new ArrayList<>();
    private boolean isGuestMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        isGuestMode = requireActivity().getIntent().getBooleanExtra("is_guest", false);

        rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        adapter = new AnnouncementAdapter(announcements);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAnnouncements.setAdapter(adapter);
        loadAnnouncements();
    }

    private void loadAnnouncements() {
        showLoading(true);
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    showLoading(false);
                    announcements.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Announcement a = new Announcement();
                        a.id = doc.getId();
                        a.title = doc.getString("title");
                        a.subtitle = doc.getString("subtitle");
                        a.category = doc.getString("category");
                        a.contact = doc.getString("contact");
                        a.date = doc.getString("date");
                        a.imageUrl = doc.getString("imageUrl");
                        a.timestamp = doc.getTimestamp("timestamp");
                        announcements.add(a);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(announcements.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to load announcements: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvAnnouncements.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showCreateAccountDialog(String message) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Create Account")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) ->
                        startActivity(new Intent(requireActivity(), RegisterActivity.class)))
                .setNegativeButton("No", null)
                .show();
    }

    public static class Announcement {
        public String id, title, subtitle, category, contact, date, imageUrl;
        public Timestamp timestamp;
    }

    public static class Comment {
        public String id, userId, userName, userAvatarUrl, text;
        public Timestamp timestamp;
    }

    private class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementVH> {
        private final List<Announcement> items;
        AnnouncementAdapter(List<Announcement> items) { this.items = items; }

        @NonNull
        @Override
        public AnnouncementVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.news_announcement, parent, false);
            return new AnnouncementVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AnnouncementVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class AnnouncementVH extends RecyclerView.ViewHolder {
            TextView tvCategory, tvTitle, tvSubtitle, tvContact, tvDate;
            ImageView ivBanner;
            RecyclerView rvComments;
            TextView tvCommentCount, tvNoComments, tvToggleComments;
            EditText etComment;
            ImageButton btnSendComment;
            LinearLayout commentSection;
            ProgressBar commentsProgress;

            private final List<Comment> comments = new ArrayList<>();
            private CommentAdapter commentAdapter;
            private boolean commentsVisible = false;
            private boolean commentsLoaded = false;

            AnnouncementVH(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvContact = itemView.findViewById(R.id.tvContact);
                tvDate = itemView.findViewById(R.id.tvDate);
                ivBanner = itemView.findViewById(R.id.ivBanner);
                rvComments = itemView.findViewById(R.id.rvComments);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                tvNoComments = itemView.findViewById(R.id.tvNoComments);
                etComment = itemView.findViewById(R.id.etComment);
                btnSendComment = itemView.findViewById(R.id.btnSendComment);
                commentSection = itemView.findViewById(R.id.commentSection);
                tvToggleComments = itemView.findViewById(R.id.tvToggleComments);
                commentsProgress = itemView.findViewById(R.id.commentsProgress);

                commentAdapter = new CommentAdapter(comments);
                rvComments.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                rvComments.setAdapter(commentAdapter);
                rvComments.setNestedScrollingEnabled(false);
            }

            void bind(Announcement announcement) {
                commentsLoaded = false;
                commentsVisible = false;
                comments.clear();
                commentAdapter.notifyDataSetChanged();
                commentSection.setVisibility(View.GONE);
                tvToggleComments.setText("💬 View Comments");

                tvTitle.setText(announcement.title != null ? announcement.title : "");
                tvSubtitle.setText(announcement.subtitle != null ? announcement.subtitle : "");
                tvCategory.setText(announcement.category != null ? announcement.category : "General");
                tvDate.setText(announcement.date != null ? announcement.date : "");

                if (announcement.contact != null && !announcement.contact.isEmpty()) {
                    tvContact.setVisibility(View.VISIBLE);
                    tvContact.setText("📞 " + announcement.contact);
                } else {
                    tvContact.setVisibility(View.GONE);
                }

                if (announcement.imageUrl != null && !announcement.imageUrl.isEmpty()
                        && announcement.imageUrl.startsWith("http")) {
                    ivBanner.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(announcement.imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(ivBanner);
                    ivBanner.setOnClickListener(v -> showFullImageDialog(announcement.imageUrl));
                } else {
                    ivBanner.setVisibility(View.GONE);
                    ivBanner.setOnClickListener(null);
                }

                tvToggleComments.setOnClickListener(v -> {
                    if (!commentsVisible) {
                        commentSection.setVisibility(View.VISIBLE);
                        tvToggleComments.setText("🔼 Hide Comments");
                        commentsVisible = true;
                        if (!commentsLoaded) loadComments(announcement.id);
                    } else {
                        commentSection.setVisibility(View.GONE);
                        tvToggleComments.setText("💬 View Comments");
                        commentsVisible = false;
                    }
                });

                btnSendComment.setOnClickListener(v -> {
                    if (isGuestMode) {
                        showCreateAccountDialog("Create account to comment on announcements?");
                        return;
                    }
                    String text = etComment.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        etComment.setError("Write something first");
                        return;
                    }
                    postComment(announcement.id, text, etComment, btnSendComment);
                });
            }

            private void loadComments(String announcementId) {
                commentsProgress.setVisibility(View.VISIBLE);
                tvNoComments.setVisibility(View.GONE);
                db.collection("announcements")
                        .document(announcementId)
                        .collection("comments")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            commentsLoaded = true;
                            commentsProgress.setVisibility(View.GONE);
                            comments.clear();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Comment c = new Comment();
                                c.id = doc.getId();
                                c.userId = doc.getString("userId");
                                c.userName = doc.getString("userName");
                                c.userAvatarUrl = doc.getString("userAvatarUrl");
                                c.text = doc.getString("text");
                                c.timestamp = doc.getTimestamp("timestamp");
                                comments.add(c);
                            }
                            commentAdapter.notifyDataSetChanged();
                            updateCommentCount(comments.size());
                            tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            commentsProgress.setVisibility(View.GONE);
                            Toast.makeText(itemView.getContext(),
                                    "Could not load comments", Toast.LENGTH_SHORT).show();
                        });
            }

            private void postComment(String announcementId, String text,
                                     EditText etComment, ImageButton btnSend) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(itemView.getContext(),
                            "Please log in to comment.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSend.setEnabled(false);
                db.collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String userName = userDoc.getString("fullName");
                            String userAvatarUrl = userDoc.getString("profileImageUrl");
                            if (userName == null || userName.isEmpty()) {
                                userName = user.getEmail() != null ? user.getEmail() : "Resident";
                            }

                            Map<String, Object> commentData = new HashMap<>();
                            commentData.put("userId", user.getUid());
                            commentData.put("userName", userName);
                            commentData.put("userAvatarUrl", userAvatarUrl != null ? userAvatarUrl : "");
                            commentData.put("text", text);
                            commentData.put("timestamp", Timestamp.now());

                            final String finalUserName = userName;
                            final String finalUserAvatarUrl = userAvatarUrl != null ? userAvatarUrl : "";

                            db.collection("announcements")
                                    .document(announcementId)
                                    .collection("comments")
                                    .add(commentData)
                                    .addOnSuccessListener(docRef -> {
                                        btnSend.setEnabled(true);
                                        etComment.setText("");
                                        Comment newComment = new Comment();
                                        newComment.id = docRef.getId();
                                        newComment.userId = user.getUid();
                                        newComment.userName = finalUserName;
                                        newComment.userAvatarUrl = finalUserAvatarUrl;
                                        newComment.text = text;
                                        newComment.timestamp = Timestamp.now();
                                        comments.add(newComment);
                                        commentAdapter.notifyItemInserted(comments.size() - 1);
                                        rvComments.scrollToPosition(comments.size() - 1);
                                        updateCommentCount(comments.size());
                                        tvNoComments.setVisibility(View.GONE);
                                    })
                                    .addOnFailureListener(e -> {
                                        btnSend.setEnabled(true);
                                        Toast.makeText(itemView.getContext(),
                                                "Failed to post comment", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(itemView.getContext(),
                                    "Could not fetch user info", Toast.LENGTH_SHORT).show();
                        });
            }

            private void updateCommentCount(int count) {
                tvCommentCount.setText(count + (count == 1 ? " Comment" : " Comments"));
            }

            private void showFullImageDialog(String imageUrl) {
                if (getContext() == null) return;
                Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.setContentView(R.layout.dialog_fullscreen_image);

                ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
                ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);

                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .fitCenter()
                        .into(ivFullscreen);

                btnClose.setOnClickListener(v -> dialog.dismiss());
                ivFullscreen.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        }
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentVH> {
        private final List<Comment> items;
        CommentAdapter(List<Comment> items) { this.items = items; }

        @NonNull
        @Override
        public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class CommentVH extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName, tvText, tvTime;

            CommentVH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
                tvName = itemView.findViewById(R.id.tvCommentName);
                tvText = itemView.findViewById(R.id.tvCommentText);
                tvTime = itemView.findViewById(R.id.tvCommentTime);
            }

            void bind(Comment comment) {
                tvName.setText(comment.userName != null ? comment.userName : "Resident");
                tvText.setText(comment.text != null ? comment.text : "");
                if (comment.timestamp != null) {
                    Date date = comment.timestamp.toDate();
                    String formatted = new SimpleDateFormat(
                            "MMM d, yyyy · h:mm a", Locale.getDefault()).format(date);
                    tvTime.setText(formatted);
                }
                if (comment.userAvatarUrl != null && !comment.userAvatarUrl.isEmpty()
                        && comment.userAvatarUrl.startsWith("http")) {
                    Glide.with(itemView.getContext())
                            .load(comment.userAvatarUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }
        }
    }
}