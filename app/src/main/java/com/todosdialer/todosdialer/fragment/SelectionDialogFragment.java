package com.todosdialer.todosdialer.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.todosdialer.todosdialer.R;

import java.util.ArrayList;

public class SelectionDialogFragment<T> extends DialogFragment {
    private ItemInterface<T> mItemInterface;
    private OnItemClickListener<T> mOnItemClickListener;

    public void setItemInterface(ItemInterface<T> itemInterface) {
        mItemInterface = itemInterface;
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mItemInterface == null) {
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selection_dialog, container, false);

        RecyclerView listView = rootView.findViewById(R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(new SelectionAdapter());

        rootView.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return rootView;
    }


    public interface ItemInterface<T> {
        ArrayList<T> getList();

        String convertString(T item);
    }


    public interface OnItemClickListener<T> {
        void onItemClicked(T item);
    }


    private class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_simple_text_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setItem(mItemInterface.getList().get(position));
        }

        @Override
        public int getItemCount() {
            return mItemInterface.getList().size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View itemContainer;
            TextView itemTextView;

            ViewHolder(View itemView) {
                super(itemView);
                itemContainer = itemView.findViewById(R.id.container);
                itemTextView = itemView.findViewById(R.id.text_item);
            }

            void setItem(final T item) {
                itemContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();

                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClicked(item);
                        }
                    }
                });

                itemTextView.setText(mItemInterface.convertString(item));
            }
        }
    }
}
