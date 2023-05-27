package com.example.myapplication.model;


import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.ConnectServerViewModel;
import com.example.myapplication.viewmodel.ConnectKernelViewModel;
import com.example.myapplication.viewmodel.VideoViewModel;
import com.example.myapplication.viewmodel.FTPViewModel;
import com.example.myapplication.viewmodel.NasViewModel;
import com.example.myapplication.viewmodel.FileManagerViewModel;


import kotlin.jvm.functions.Function0;

public class ViewModelProviderFactories<T extends ViewModel> {

    private Class<T> mType;

    public ViewModelProviderFactories(Class<T> type) {
        mType = type;
    }

    ViewModelProvider.Factory getFactory(Function0 activitySupplier) {
        ViewModelProvider.Factory result = null;
        if (ConnectKernelViewModel.class.equals(mType)) {
            result = new ConnectKernelViewModelFactory();
        } else if (ConnectServerViewModel.class.equals(mType)) {
            result = new ConnectServerViewModelFactory();
        } else if (VideoViewModel.class.equals(mType)) {
            result = new VideoViewModelFactory(activitySupplier);
        } else if (FTPViewModel.class.equals(mType)) {
            result = new FTPViewModelFactory(activitySupplier);
        } else if (NasViewModel.class.equals(mType)) {
            result = new NasViewModelFactory(activitySupplier);
        } else if (FileManagerViewModel.class.equals(mType)) {
            result = new FileManagerViewModelFactory();
        } else {

        }
        return result;
    }
}

class ConnectKernelViewModelFactory implements ViewModelProvider.Factory {
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConnectKernelViewModel();
    }
}

class ConnectServerViewModelFactory implements ViewModelProvider.Factory {
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConnectServerViewModel();
    }
}

class VideoViewModelFactory implements ViewModelProvider.Factory {
    Function0 mActivitySupplier;

    public VideoViewModelFactory(Function0 activitySupplier) {
        mActivitySupplier = activitySupplier;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new VideoViewModel();
    }
}


class FTPViewModelFactory implements ViewModelProvider.Factory {

    Function0 mActivitySupplier;

    public FTPViewModelFactory(Function0 activitySupplier) {
        mActivitySupplier = activitySupplier;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new FTPViewModel(mActivitySupplier);
    }
}

class NasViewModelFactory implements ViewModelProvider.Factory {

    Function0 mActivitySupplier;

    public NasViewModelFactory (Function0 activitySupplier) {
        mActivitySupplier = activitySupplier;
    }


    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NasViewModel(mActivitySupplier);
    }
}

class FileManagerViewModelFactory implements ViewModelProvider.Factory {
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new FileManagerViewModel();
    }
}
