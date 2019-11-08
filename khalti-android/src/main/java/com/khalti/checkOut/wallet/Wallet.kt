package com.khalti.checkOut.wallet

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.khalti.R
import com.khalti.signal.Signal
import com.khalti.utils.*
import kotlinx.android.synthetic.main.wallet_form.view.*
import java.util.*
import kotlin.math.abs

class Wallet : Fragment(), WalletContract.View {

    private var progressDialog: AppCompatDialog? = null
    private lateinit var fragmentActivity: FragmentActivity
    private lateinit var presenter: WalletContract.Presenter

    private var height: Int = 0
    private lateinit var mainView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mainView = inflater.inflate(R.layout.wallet_form, container, false)
        fragmentActivity = activity!!
        presenter = WalletPresenter(this)

        presenter.onCreate()

        return mainView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }

    override val payButtonText: String get() = ViewUtil.getText(mainView.btnPay)

    override val formData: Map<String, String>
        get() = object : HashMap<String, String>() {
            init {
                put("mobile", ViewUtil.getText(mainView.etMobile))
                put("code", ViewUtil.getText(mainView.etCode))
                put("pin", ViewUtil.getText(mainView.etPIN))
            }
        }

    override fun toggleProgressDialog(action: String, show: Boolean): Signal<Boolean> {
        var signal = Signal<Boolean>()
        var message = ""
        when (action) {
            "init" -> message = ResourceUtil.getString(fragmentActivity, R.string.init_payment)
            "confirm" -> message = ResourceUtil.getString(fragmentActivity, R.string.confirming_payment)
        }

        if (show) {
            val pair = UserInterfaceUtil.runProgressDialog(fragmentActivity, message, ResourceUtil.getString(fragmentActivity, R.string.please_wait))
            progressDialog = pair.first
            signal = pair.second
        } else {
            progressDialog?.dismiss()
        }
        return signal
    }

    override fun toggleConfirmationLayout(show: Boolean) {
        val buttonText = if (show) ResourceUtil.getString(fragmentActivity, R.string.confirm_payment) else "Pay Rs ${NumberUtil.convertToRupees(Store.getConfig().amount)}"
        mainView.btnPay?.text = buttonText
        mainView.etCode?.setText("")
        mainView.etPIN?.setText("")
        mainView.elConfirmation.toggleExpansion()
    }

    override fun togglePinMessage(show: Boolean) {
        mainView.cvPinMessage?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun setPinMessage(message: String) {
        mainView.tvPinMessage?.text = message + " " + ResourceUtil.getString(fragmentActivity, R.string.sms_info)
    }

    override fun setMobile(mobile: String) {
        mainView.etMobile?.setText(mobile)
        mainView.etMobile?.setSelection(mobile.length)
    }

    override fun setEditTextError(view: String, error: String?) {
        val isError = EmptyUtil.isNotNull(error)
        when (view) {
            "mobile" -> {
                mainView.tilMobile?.isErrorEnabled = isError
                mainView.tilMobile?.error = error
            }
            "code" -> {
                mainView.llCode.measure(0, 0)
                val beforeA = mainView.llCode.measuredHeight

                mainView.tilCode.isErrorEnabled = isError
                mainView.tilCode.error = error

                mainView.llCode.measure(0, 0)
                val afterA = mainView.llCode.measuredHeight

                height = abs(height + (afterA - beforeA))
            }
            "pin" -> {
                mainView.llPIN.measure(0, 0)
                val beforeB = mainView.llPIN.measuredHeight

                mainView.tilPIN.isErrorEnabled = isError
                mainView.tilPIN.error = error

                mainView.llPIN.measure(0, 0)
                val afterB = mainView.llPIN.measuredHeight

                height = abs(height + (afterB - beforeB))
            }
        }
    }

    override fun setButtonText(text: String) {
        mainView.btnPay?.text = text
    }

    override fun setConfirmationLayoutHeight(view: String) {
        val til: TextInputLayout
        val ll: LinearLayout
        if (view == "code") {
            til = mainView.tilCode
            ll = mainView.llCode
        } else {
            til = mainView.tilPIN
            ll = mainView.llPIN
        }
        if (EmptyUtil.isNotNull(til?.error)) {
            ll?.measure(0, 0)
            val beforeA = ll?.measuredHeight

            til?.setErrorEnabled(false)

            ll?.measure(0, 0)
            val afterA = ll?.measuredHeight

            if (EmptyUtil.isNotNull(beforeA) && EmptyUtil.isNotNull(afterA)) {
                height = abs(height + (beforeA!! - afterA!!))

                val layoutParams = mainView.llConfirmation.layoutParams as ExpandableLayout.LayoutParams
                layoutParams.height = mainView.llConfirmation.height - height
                mainView.llConfirmation.layoutParams = layoutParams

                height = 0
            }
        }
    }

    override fun setEditTextListener(): Map<String, Signal<CharSequence>> = object : HashMap<String, Signal<CharSequence>>() {
        init {
            put("mobile", ViewUtil.setTextChangeListener(mainView.etMobile))
            put("code", ViewUtil.setTextChangeListener(mainView.etCode))
            put("pin", ViewUtil.setTextChangeListener(mainView.etPIN))
        }
    }

    override fun setClickListener(): Map<String, Signal<Any>> = object : HashMap<String, Signal<Any>>() {
        init {
            put("pay", ViewUtil.setClickListener(mainView.btnPay))
            put("khalti", ViewUtil.setClickListener(mainView.ivKhalti))
        }
    }

    override fun showNetworkError() {
        UserInterfaceUtil.showSnackBar(fragmentActivity, (this.fragmentActivity as com.khalti.checkOut.CheckOutActivity).cdlMain, ResourceUtil.getString(fragmentActivity, R.string.network_error_body), null, Snackbar.LENGTH_LONG)
    }

    override fun showMessageDialog(title: String, message: String) {
        UserInterfaceUtil.showInfoDialog(fragmentActivity, title, message, autoDismiss = true, cancelable = true, positiveText = ResourceUtil.getString(fragmentActivity, R.string.got_it), negativeText = null)
    }

    override fun showPINDialog(title: String, message: String): Signal<Boolean> =
            UserInterfaceUtil.showInfoDialog(fragmentActivity, title, message, autoDismiss = true, cancelable = true, positiveText = ResourceUtil.getString(fragmentActivity, R.string.ok), negativeText = ResourceUtil.getString(fragmentActivity, R.string.cancel))

    override fun showSlogan() {
        Toast.makeText(fragmentActivity, ResourceUtil.getString(fragmentActivity, R.string.slogan), Toast.LENGTH_SHORT).show()
    }

    override fun showBranding() {
        mainView.llKhaltiBranding?.visibility = View.VISIBLE
    }

    override fun openKhaltiSettings() {
        var i: Intent?
        val manager = fragmentActivity.packageManager
        try {
            i = manager.getLaunchIntentForPackage("com.khalti.red")
            i = if (EmptyUtil.isNotNull(i)) i else manager.getLaunchIntentForPackage("com.khalti")
            if (EmptyUtil.isNull(i)) {
                throw PackageManager.NameNotFoundException()
            }
            i!!.addCategory(Intent.CATEGORY_LAUNCHER)

            val map = HashMap<String, String>()
            map["pin_settings"] = "pin_settings"
            i.putExtra("map", map)

            startActivity(i)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()

            /*presenter.showPINInBrowserDialog("Error", ResourceUtil.getString(fragmentActivity, R.string.khalti_not_found) + "\n\n" +
                    ResourceUtil.getString(fragmentActivity, R.string.set_pin_in_browser))*/
        }

    }

    override fun openLinkInBrowser(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    override fun closeWidget() {
        fragmentActivity.finish()
    }

    override fun updateConfirmationHeight() {
        val layoutParams = mainView.llConfirmation.layoutParams as ExpandableLayout.LayoutParams
        layoutParams.height = mainView.llConfirmation.height + height
        mainView.llConfirmation.layoutParams = layoutParams

        height = 0
    }

    override fun getMessage(action: String): String {
        return when (action) {
            "pin_not_set" -> ResourceUtil.getString(fragmentActivity, R.string.pin_not_set)
            "pin_not_set_continue" -> ResourceUtil.getString(fragmentActivity, R.string.pin_not_set_continue)
            "khalti_not_found" -> ResourceUtil.getString(fragmentActivity, R.string.khalti_not_found)
            "set_pin_in_browser" -> ResourceUtil.getString(fragmentActivity, R.string.set_pin_in_browser)
            else -> ""
        }
    }

    override fun hasNetwork(): Boolean {
        return NetworkUtil.isNetworkAvailable(fragmentActivity)
    }

    override fun doesPackageExist(): Boolean {
        val manager = fragmentActivity.packageManager
        return try {
            var i = manager.getLaunchIntentForPackage("com.khalti.red")
            if (EmptyUtil.isNull(i)) {
                i = manager.getLaunchIntentForPackage("com.khalti")
            }

            EmptyUtil.isNotNull(i)

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    override fun setPresenter(presenter: WalletContract.Presenter) {
        this.presenter = presenter
    }
}