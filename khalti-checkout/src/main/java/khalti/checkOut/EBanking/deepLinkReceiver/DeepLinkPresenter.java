package khalti.checkOut.EBanking.deepLinkReceiver;

import android.support.annotation.NonNull;

import com.rxbus.RxBus;
import com.utila.EmptyUtil;
import com.utila.GuavaUtil;

import java.util.HashMap;

import khalti.checkOut.api.Config;
import khalti.checkOut.api.OnCheckOutListener;
import khalti.utils.DataHolder;

class DeepLinkPresenter implements DeepLinkContract.Listener {
    @NonNull
    private final DeepLinkContract.View mDeepLinkView;

    DeepLinkPresenter(@NonNull DeepLinkContract.View mDeepLinkView) {
        this.mDeepLinkView = GuavaUtil.checkNotNull(mDeepLinkView);
        mDeepLinkView.setListener(this);
    }

    @Override
    public void receiveEBankingData() {
        HashMap<String, Object> map = mDeepLinkView.receiveEBankingData();
        Config config = EmptyUtil.isNotNull(DataHolder.getConfig()) ? DataHolder.getConfig() : mDeepLinkView.getConfigFromFile();
        if (EmptyUtil.isNotNull(map) && EmptyUtil.isNotNull(config)) {
            OnCheckOutListener onCheckOutListener = config.getOnCheckOutListener();
            onCheckOutListener.onSuccess(map);
        }
        mDeepLinkView.closeDeepLink();
        RxBus.getInstance().post("close_check_out", null);
    }
}
