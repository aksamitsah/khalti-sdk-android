package com.khalti.checkOut.wallet

import com.khalti.checkOut.api.ErrorAction
import com.khalti.checkOut.api.Result
import com.khalti.checkOut.helper.Config
import com.khalti.checkOut.wallet.helper.WalletInitPojo
import com.khalti.signal.CompositeSignal
import com.khalti.utils.*
import com.khalti.utils.ErrorUtil.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WalletPresenter(view: WalletContract.View) : WalletContract.Presenter {

    private val view: WalletContract.View = GuavaUtil.checkNotNull<WalletContract.View>(view)
    private lateinit var config: Config
    private val compositeSignal = CompositeSignal()
    private val model = WalletModel()

    private val parentJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + parentJob)

    private var pinWebLink: String? = null
    private var clicks = 0

    private var mobile = ""

    private lateinit var walletInitPojo: WalletInitPojo

    init {
        view.setPresenter(this)
    }

    override fun onCreate() {
        config = Store.getConfig()
        val mobile = config.mobile
        if (EmptyUtil.isNotNull(mobile) && EmptyUtil.isNotEmpty(mobile) && ValidationUtil.isMobileNumberValid(mobile)) {
            view.setMobile(mobile!!)
        }

        view.setButtonText("Pay Rs " + StringUtil.formatNumber(NumberUtil.convertToRupees(config.amount)))
        view.showBranding()

        val clickMap = view.setClickListener()
        if (EmptyUtil.isNotNull(clickMap["pay"])) {
            compositeSignal.add(clickMap.getValue("pay").connect {

                val dataMap = view.formData
                if (view.payButtonText.toLowerCase().contains("confirm")) {
                    if (isFinalFormValid(dataMap.getValue("code"))) {
                        onConfirmPayment(view.hasNetwork(), dataMap.getValue("code"), dataMap.getValue("pin"))
                    } else {
                        view.updateConfirmationHeight()
                    }
                } else {
                    if (isInitialFormValid(dataMap.getValue("mobile"), dataMap.getValue("pin"))) {
                        onInitiatePayment(view.hasNetwork(), dataMap.getValue("mobile"), dataMap.getValue("pin"))
                    }
                }
            })
        }

        if (EmptyUtil.isNotNull(clickMap["khalti"])) {
            compositeSignal.add(clickMap.getValue("khalti").connect {
                clicks += 1
                if (clicks > 2) {
                    clicks = 0
                    view.showSlogan()
                }
            })
        }

        val watcherMap = view.setEditTextListener()
        if (EmptyUtil.isNotNull(watcherMap["mobile"])) {
            compositeSignal.add(watcherMap.getValue("mobile")
                    .connect {
                        view.setEditTextError("mobile", null)
                        if (view.payButtonText.toLowerCase().contains("confirm")) {
                            view.toggleConfirmationLayout(false)
                        }
                    })
        }

        if (EmptyUtil.isNotNull(watcherMap["pin"])) {
            compositeSignal.add(watcherMap.getValue("pin")
                    .connect {
                        view.setEditTextError("pin", null)
                        if (view.payButtonText.toLowerCase().contains("confirm")) {
                            view.toggleConfirmationLayout(false)
                        }
                    })
        }

        if (EmptyUtil.isNotNull(watcherMap["code"])) {
            compositeSignal.add(watcherMap.getValue("code")
                    .connect {
                        view.setEditTextError("code", null)
                        view.setConfirmationLayoutHeight()
                    })
        }
    }

    override fun onDestroy() {
        compositeSignal.clear()
        parentJob.cancel()
    }

    override fun isInitialFormValid(mobile: String, pin: String): Boolean {
        var mobileError: String? = null
        var pinError: String? = null

        if (EmptyUtil.isNotEmpty(mobile)) {
            if (!ValidationUtil.isMobileNumberValid(mobile)) {
                mobileError = MOBILE_ERROR
            }
        } else {
            mobileError = EMPTY_ERROR
        }

        if (EmptyUtil.isNotEmpty(pin)) {
            if (pin.length < 4) {
                pinError = PIN_ERROR
            }
        } else {
            pinError = EMPTY_ERROR
        }

        view.setEditTextError("mobile", mobileError)
        view.setEditTextError("pin", pinError)

        return EmptyUtil.isNull(mobileError) && EmptyUtil.isNull(pinError)
    }

    override fun isFinalFormValid(confirmationCode: String): Boolean {
        var codeError: String? = null

        if (EmptyUtil.isNotEmpty(mobile)) {
            if (confirmationCode.length < 6) {
                codeError = CODE_ERROR
            }
        } else {
            codeError = EMPTY_ERROR
        }

        view.setEditTextError("code", codeError)

        return EmptyUtil.isNull(codeError)
    }

    override fun onInitiatePayment(isNetwork: Boolean, mobile: String, pin: String) {
        if (isNetwork) {
            this.mobile = mobile
            scope.launch {
                view.toggleProgressDialog("init", true)
                when (val result = model.initiatePayment(mobile, pin, config)) {
                    is Result.Success -> {
                        walletInitPojo = result.data
                        view.toggleProgressDialog("init", false)
                        view.toggleConfirmationLayout(true)
                    }
                    is Result.Error -> {
                        view.toggleProgressDialog("init", false)
                        var message = result.throwable.message
                        if (EmptyUtil.isNotNull(message) && message!!.contains("</a>")) {
                            pinWebLink = HtmlUtil.getHrefLink(message)

                            compositeSignal.add(view.showPINDialog("Error", view.getMessage("pin_not_set") + "\n\n" + view.getMessage("pin_not_set_continue"))
                                    .connect { it ->
                                        if (it) {
                                            if (view.doesPackageExist()) {
                                                view.openKhaltiSettings()
                                            } else {
                                                compositeSignal.add(view.showPINDialog("Error", view.getMessage("khalti_not_found") + "\n\n" + view.getMessage("set_pin_in_browser"))
                                                        .connect {
                                                            if (it && EmptyUtil.isNotNull(pinWebLink)) {

                                                                view.openLinkInBrowser(Constant.url + pinWebLink!!.substring(1))
                                                            }
                                                        })
                                            }
                                        }
                                    })
//                            config.onCheckOutListener.onError(ErrorAction.WALLET_INITIATE.action, message)/TODO
                        } else {
                            if (EmptyUtil.isNull(message)) {
                                message = ""
                            }
                            view.showMessageDialog("Error", message!!)
                        }
                    }
                }
            }
        } else {
            view.showNetworkError()
        }
    }

    override fun onConfirmPayment(isNetwork: Boolean, confirmationCode: String, transactionPin: String) {
        if (isNetwork) {
            scope.launch {
                if (EmptyUtil.isNotNull(walletInitPojo) && EmptyUtil.isNotNull(walletInitPojo.token)) {
                    view.toggleProgressDialog("confirm", true)
                    when (val result = model.confirmPayment(confirmationCode, transactionPin, walletInitPojo.token!!)) {
                        is Result.Success -> {
                            view.toggleProgressDialog("confirm", false)
                            val data = HashMap<String, Any>()
                            if (EmptyUtil.isNotNull(config.additionalData) && EmptyUtil.isNotEmpty(config.additionalData)) {
                                val walletConfirmPojo = result.data
                                data.putAll(config.additionalData!!)
                                data["mobile"] = mobile
                                if (EmptyUtil.isNotNull(walletConfirmPojo.amount)) {
                                    data["amount"] = walletConfirmPojo.amount!!
                                }
                                if (EmptyUtil.isNotNull(walletConfirmPojo.productUrl)) {
                                    data["product_url"] = walletConfirmPojo.productUrl!!
                                }
                                if (EmptyUtil.isNotNull(walletConfirmPojo.token)) {
                                    data["token"] = walletConfirmPojo.token!!
                                }
                                if (EmptyUtil.isNotNull(walletConfirmPojo.productName)) {
                                    data["product_name"] = walletConfirmPojo.productName!!
                                }
                                if (EmptyUtil.isNotNull(walletConfirmPojo.productIdentity)) {
                                    data["product_identity"] = walletConfirmPojo.productIdentity!!
                                }

                                config.onSuccessListener?.onSuccess(data)
                                view.closeWidget()
                            }

                        }

                        is Result.Error -> {
                            var message = result.throwable.message
                            if (EmptyUtil.isNull(message)) {
                                message = ""
                            }
                            view.toggleProgressDialog("confirm", false)
                            view.showMessageDialog("Error", message!!)
//                            config.onCheckOutListener.onError(ErrorAction.WALLET_CONFIRM.action, message)//TODO
                        }

                    }
                }
            }
        } else {
            view.showNetworkError()
        }
    }
}