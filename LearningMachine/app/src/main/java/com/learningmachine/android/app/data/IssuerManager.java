package com.learningmachine.android.app.data;

import android.content.Context;

import com.learningmachine.android.app.data.error.IssuerAnalyticsException;
import com.learningmachine.android.app.data.model.IssuerRecord;
import com.learningmachine.android.app.data.store.IssuerStore;
import com.learningmachine.android.app.data.webservice.IssuerService;
import com.learningmachine.android.app.data.webservice.request.IssuerAnalytic;
import com.learningmachine.android.app.data.webservice.request.IssuerIntroductionRequest;
import com.learningmachine.android.app.data.webservice.response.IssuerResponse;
import com.learningmachine.android.app.util.GsonUtil;
import com.learningmachine.android.app.util.StringUtils;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import timber.log.Timber;

public class IssuerManager {

    private IssuerStore mIssuerStore;
    private IssuerService mIssuerService;

    public IssuerManager(IssuerStore issuerStore, IssuerService issuerService) {
        mIssuerStore = issuerStore;
        mIssuerService = issuerService;
    }

    public Observable<Void> loadSampleIssuer(Context context) {
        try {
            GsonUtil gsonUtil = new GsonUtil(context);
            IssuerResponse issuerResponse = gsonUtil.loadModelObject("sample-issuer", IssuerResponse.class);
            mIssuerStore.saveIssuerResponse(issuerResponse);
            return Observable.just(null);
        } catch (IOException e) {
            Timber.e(e, "Unable to load Sample Issuer");
            return Observable.error(e);
        }
    }

    public Observable<IssuerRecord> getIssuer(String issuerUuid) {
        return Observable.just(mIssuerStore.loadIssuer(issuerUuid));
    }

    public Observable<IssuerRecord> getIssuerForCertificate(String certUuid) {
        return Observable.just(mIssuerStore.loadIssuerForCertificate(certUuid));
    }

    public Observable<List<IssuerRecord>> getIssuers() {
        return Observable.just(mIssuerStore.loadIssuers());
    }

    public Observable<String> addIssuer(String url, String bitcoinAddress, String nonce) {
        IssuerIntroductionRequest request = new IssuerIntroductionRequest(bitcoinAddress, nonce);
        return mIssuerService.getIssuer(url)
                .flatMap(issuer -> Observable.combineLatest(Observable.just(issuer),
                        mIssuerService.postIntroduction(issuer.getIntroUrl(), request),
                        (issuer1, aVoid) -> issuer1))
                .map(issuerResponse -> {
                    mIssuerStore.saveIssuerResponse(issuerResponse);
                    return issuerResponse.getUuid();
                });
    }

    public Observable<Void> certificateViewed(String certUuid) {
        return sendAnalytics(certUuid, IssuerAnalytic.Action.VIEWED);
    }

    public Observable<Void> certificateVerified(String certUuid) {
        return sendAnalytics(certUuid, IssuerAnalytic.Action.VERIFIED);
    }

    public Observable<Void> certificateShared(String certUuid) {
        return sendAnalytics(certUuid, IssuerAnalytic.Action.SHARED);
    }

    private Observable<Void> sendAnalytics(String certUuid, IssuerAnalytic.Action action) {
        return getIssuerForCertificate(certUuid).flatMap(issuer -> {
            String issuerAnalyticsUrlString = issuer.getAnalyticsUrlString();
            if (StringUtils.isEmpty(issuerAnalyticsUrlString)) {
                return Observable.error(new IssuerAnalyticsException());
            }
            IssuerAnalytic issuerAnalytic = new IssuerAnalytic(certUuid, action);
            return mIssuerService.postIssuerAnalytics(issuerAnalyticsUrlString, issuerAnalytic);
        });
    }
}
