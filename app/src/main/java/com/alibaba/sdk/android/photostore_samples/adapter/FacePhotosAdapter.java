/**
 * Copyright (C) 2017 Alibaba Group Holding Limited
 */
package com.alibaba.sdk.android.photostore_samples.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import com.alibaba.sdk.android.photostore_samples.BusProvider;
import com.alibaba.sdk.android.photostore_samples.R;
import com.alibaba.sdk.android.photostore_samples.constants.Constants;
import com.alibaba.sdk.android.photostore_samples.constants.FragmentType;
import com.alibaba.sdk.android.photostore_samples.controller.PhotosController;
import com.alibaba.sdk.android.photostore_samples.event.OnFinishActionModeEvent;
import com.alibaba.sdk.android.photostore_samples.event.OnStartActionModeEvent;
import com.alibaba.sdk.android.photostore_samples.model.MyPhoto;
import com.alibaba.sdk.android.photostore_samples.util.DataRunner;
import com.alibaba.sdk.android.photostore_samples.util.ThumbnailLoader;
import com.alibaba.sdk.android.photostore_samples.view.MoveActivity;
import com.alibaba.sdk.android.photostore_samples.view.PreviewActivity;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class FacePhotosAdapter extends HeaderFooterAdapter {

    private static final String TAG = FacePhotosAdapter.class.getSimpleName();

    private enum TYPE {
        HEADER,
        FOOTER,
        CONTENT_SPAN,
        CONTENT
    }

    private List<MyPhoto> photos;
    private List<MyPhoto> tPhotos;

    boolean firstLoaded = true;

    long faceId = 0;

    public FacePhotosAdapter(Context context, int gridCount) {
        super(context, gridCount);
        photos = new ArrayList<>();
        tPhotos = new ArrayList<>();

        BusProvider.getInstance().register(this);
    }

    @Override
    public void destroy() {
        BusProvider.getInstance().unregister(this);
    }

    public void clear() {
        faceId = 0;
        photos.clear();
        selects.clear();
        notifyDataSetChanged();
        setLoading(true);
    }

    /**
     * call from main thread
     * @param cloudphotos
     */
    public void setCloudPhotos(final List<MyPhoto> cloudphotos) {
        DataRunner.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                tPhotos.clear();
                tPhotos.addAll(cloudphotos);
                selects.clear();
                for (MyPhoto p : cloudphotos) {
                    selects.add(false);
                }
            }
        });
    }

    public void setFaceId(final long faceId) {
        this.faceId = faceId;
    }

    public void movePhotosToFace() {
        MoveActivity.launch(mContext, FragmentType.FACE_PHOTOS, faceId, getSelectedIds());
    }

    public void buildData(final BuildDataCallback callback) {
        DataRunner.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "buildPositionMap");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        photos.clear();
                        photos.addAll(tPhotos);

                        if (firstLoaded) {
                            if (!hasData()) {
                                // do nothing, still loading
                            } else {
                                setLoading(false);
                            }
                            firstLoaded = false;
                        } else {
                            setLoading(false);
                        }

                        notifyDataSetChanged();

                        if (callback != null) {
                            callback.onComplete();
                        }
                    }
                });

            }
        });

    }

    @Override
    protected RecyclerView.ViewHolder onCreateContentViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE.CONTENT_SPAN.ordinal()) {
        } else if (viewType == TYPE.CONTENT.ordinal()) {
            return new PhotoViewHolder((ViewGroup) mLayoutInflater.inflate(R.layout.grid_image, parent, false));
        }
        return null;
    }

    @Override
    protected void onBindContentViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PhotoViewHolder) {
            final MyPhoto p = photos.get(position);
            if (p == null) {
                return;
            }

            final PhotoViewHolder h = (PhotoViewHolder) holder;
            h.position = position;
            ThumbnailLoader.getInstance().loadByPhotoId(h.ivPhoto, p.id, Constants.PHOTO_WIDTH, Constants.PHOTO_HEIGHT);

            if (isActionMode) {
                h.checkBox.setVisibility(View.VISIBLE);
                if (selects.get(position)) {
                    h.checkBox.setButtonDrawable(R.drawable.btn_check_on);
                    h.ivPhoto.setScaleX(0.8f);
                    h.ivPhoto.setScaleY(0.8f);
                } else {
                    h.checkBox.setButtonDrawable(R.drawable.btn_check_off);
                    h.ivPhoto.setScaleX(1f);
                    h.ivPhoto.setScaleY(1f);
                }
            } else {
                h.checkBox.setVisibility(View.INVISIBLE);
                h.ivPhoto.setScaleX(1f);
                h.ivPhoto.setScaleY(1f);
            }
        }
    }

    @Override
    protected int getContentItemCount() {
        return photos == null ? 0 : photos.size();
    }

    @Override
    protected int getContentItemViewType(int position) {
        return TYPE.CONTENT.ordinal();
    }

    @Override
    public int getContentSpanSize(int position) {
        return 1;
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_photo)
        ImageView ivPhoto;

        @BindView(R.id.checkBox)
        CheckBox checkBox;

        int position = 0;

        public PhotoViewHolder(ViewGroup viewGroup) {
            super(viewGroup);
            ButterKnife.bind(this, viewGroup);
        }

        void toggle() {
            if (selects.get(position)) {
                checkBox.setButtonDrawable(R.drawable.btn_check_off);
                ivPhoto.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            } else {
                checkBox.setButtonDrawable(R.drawable.btn_check_on);
                ivPhoto.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start();
            }
            selects.set(position, !selects.get(position));
        }

        @OnClick(R.id.iv_photo)
        void onItemClick() {
            if (isActionMode) {
                toggle();
            } else {
                List<Long> list = getPhotos();
                MyPhoto myP = photos.get(position);
                if (myP == null) {
                    return;
                }

                int pos = 0;
                for (int i = 0; i < list.size(); i++) {
                    Long pid = list.get(i);
                    if (pid == myP.id) {
                        pos = i;
                        break;
                    }
                }

                PhotosController.getInstance().setPhotoList(getPhotos());
                PreviewActivity.launch(mContext, pos);
            }
        }

        @OnClick(R.id.checkBox)
        void onCheckClick() {
            toggle();
        }

        @OnLongClick(R.id.iv_photo)
        boolean onItemLongClick() {
            if (isReadOnly) {
                return false;
            }
            if (!isActionMode) {
                startActionMode();
                toggle();
                return true;
            }
            return false;
        }
    }

    @Override
    public void startActionMode() {
        isActionMode = true;
        selects.clear();
        for (MyPhoto p : photos) {
            selects.add(false);
        }
        BusProvider.getInstance().post(new OnStartActionModeEvent());
        notifyDataSetChanged();
    }

    @Override
    public void finishActionMode() {
        isActionMode = false;
        selects.clear();
        notifyDataSetChanged();
    }

    @Subscribe
    public void onStartActionMode(OnStartActionModeEvent event) {
    }

    @Subscribe
    public void onFinishActoinMode(OnFinishActionModeEvent event) {
        finishActionMode();
    }

    public List<MyPhoto> getSelected() {
        List<MyPhoto> list = new ArrayList<>();
        for (int i = 0; i < selects.size(); i++) {
            if (selects.get(i)) {
                MyPhoto p = photos.get(i);
                if (p != null) {
                    list.add(new MyPhoto(photos.get(i)));
                }
            }
        }
        return list;
    }

    private List<Long> getSelectedIds() {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < selects.size(); i++) {
            if (selects.get(i)) {
                MyPhoto p = photos.get(i);
                if (p != null) {
                    list.add(p.id);
                }
            }
        }
        return list;
    }

    public List<Long> getPhotos() {
        List<Long> list = new ArrayList<>();
        for (MyPhoto p : photos) {
            if (p != null) {
                list.add(p.id);
            }
        }
        return list;
    }

    public interface BuildDataCallback {
        void onComplete();
    }

}

