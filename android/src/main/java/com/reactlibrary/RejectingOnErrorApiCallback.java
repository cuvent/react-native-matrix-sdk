package com.reactlibrary;

import com.facebook.react.bridge.Promise;

import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;

import static com.reactlibrary.MatrixSdkModule.E_MATRIX_ERROR;
import static com.reactlibrary.MatrixSdkModule.E_NETWORK_ERROR;
import static com.reactlibrary.MatrixSdkModule.E_UNEXCPECTED_ERROR;

public abstract class RejectingOnErrorApiCallback<T> implements ApiCallback<T> {
    private final Promise promise;

    public RejectingOnErrorApiCallback(Promise promise) {
        this.promise = promise;
    }

    @Override
    public void onNetworkError(Exception e) {
        promise.reject(E_NETWORK_ERROR, e.getMessage());
    }

    @Override
    public void onMatrixError(MatrixError e) {
        promise.reject(E_MATRIX_ERROR, e.getMessage());
    }

    @Override
    public void onUnexpectedError(Exception e) {
        promise.reject(E_UNEXCPECTED_ERROR, e.getMessage());
    }
}
