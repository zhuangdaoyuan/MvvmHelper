package com.zhixinhuixue.library.common.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.kingja.loadsir.core.LoadService
import com.kingja.loadsir.core.LoadSir
import com.zhixinhuixue.library.common.ext.*
import com.zhixinhuixue.library.common.state.EmptyCallback
import com.zhixinhuixue.library.common.state.ErrorCallback
import com.zhixinhuixue.library.common.state.LoadingCallback
import com.zhixinhuixue.library.net.entity.base.LoadStatusEntity
import com.zhixinhuixue.library.net.entity.base.LoadingDialogEntity
import com.zhixinhuixue.library.net.entity.loadingtype.LoadingType

/**
 * 作者　: hegaojian
 * 时间　: 2020/11/18
 * 描述　:
 */
abstract class BaseVmFragment<VM : BaseViewModel> : BaseFragment(), BaseIView {

    //界面状态管理者
    lateinit var uiStatusManger: LoadService<*>

    //是否第一次加载
    private var isFirst: Boolean = true

    //当前Fragment绑定的泛型类ViewModel
    lateinit var mViewModel: VM

    //父类activity
    lateinit var mActivity: AppCompatActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isFirst = true
        javaClass.simpleName.logD()
        val rootView = if (dataBindView == null) {
            inflater.inflate(layoutId, container, false)
        } else {
            dataBindView
        }
        return if (getLoadingView() == null) {
            uiStatusManger = LoadSir.getDefault().register(rootView) {
                onLoadRetry()
            }
            container?.removeView(uiStatusManger.loadLayout)
            uiStatusManger.loadLayout
        } else {
            rootView
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as AppCompatActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = createViewModel()
        initStatusView(view, savedInstanceState)
        initLoadingUiChange()
        initObserver()
        onRequestSuccess()
        onBindViewClick()
    }

    private fun initStatusView(view: View, savedInstanceState: Bundle?) {
        getLoadingView()?.let {
            uiStatusManger = LoadSir.getDefault().register(it) {
                onLoadRetry()
            }
        }
        view.post {
            initView(savedInstanceState)
        }
    }

    /**
     * 创建viewModel
     */
    private fun createViewModel(): VM {
        return ViewModelProvider(this).get(getVmClazz(this))
    }

    /**
     * 初始化view操作
     */
    abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 懒加载
     */
    open fun lazyLoadData() {}

    /**
     * 创建观察者
     */
    open fun initObserver() {}

    override fun onResume() {
        super.onResume()
        onVisible()
    }

    /**
     * 是否需要懒加载
     */
    private fun onVisible() {
        if (lifecycle.currentState == Lifecycle.State.STARTED && isFirst) {
            view?.post {
                lazyLoadData()
                isFirst = false
            }
        }
    }

    /**
     * 子类可传入需要被包裹的View，做状态显示-空、错误、加载
     * 如果子类不覆盖该方法 那么会将整个当前Fragment界面都当做View包裹
     */
    open fun getLoadingView(): View? {
        return null
    }

    /**
     * 点击事件方法 配合setOnclick()拓展函数调用，做到黄油刀类似的点击事件
     */
    open fun onBindViewClick() {}

    /**
     * 注册 UI 事件
     */
    private fun initLoadingUiChange() {
        mViewModel.loadingChange.run {
            loading.observeInFragment(this@BaseVmFragment) {
                if (it.loadingType == LoadingType.LOADING_DIALOG) {
                    if (it.isShow) {
                        showLoadingExt()
                    } else {
                        dismissLoadingExt()
                    }
                    return@observeInFragment
                }
                if (it.loadingType == LoadingType.LOADING_CUSTOM) {
                    if (it.isShow) {
                        showCustomLoading(it)
                    } else {
                        dismissCustomLoading(it)
                    }
                    return@observeInFragment
                }
                if (it.loadingType == LoadingType.LOADING_XML) {
                    if (it.isShow) {
                        showLoadingUi()
                    }
                    return@observeInFragment
                }
            }
            showEmpty.observeInFragment(this@BaseVmFragment) {
                onRequestEmpty(it)
            }
            showError.observeInFragment(this@BaseVmFragment) {
                if (it.loadingType == LoadingType.LOADING_XML) {
                    showErrorUi(it.errorMessage)
                }
                onRequestError(it)
            }
            showSuccess.observeInFragment(this@BaseVmFragment) {
                showSuccessUi()
            }
        }
    }

    /**
     * 请求列表数据为空时 回调
     * @param loadStatus LoadStatusEntity
     */
    override fun onRequestEmpty(loadStatus: LoadStatusEntity) {
        showEmptyUi()
    }

    /**
     * 请求接口失败回调，如果界面有请求接口，需要处理错误业务，请实现它 乳沟不实现那么 默认吐司错误消息
     * @param loadStatus LoadStatusEntity
     */
    override fun onRequestError(loadStatus: LoadStatusEntity) {
        loadStatus.errorMessage.toast()
    }

    /**
     * 请求成功的回调放在这里面 没干啥就是取了个名字，到时候好找
     */
    override fun onRequestSuccess() {

    }

    /**
     * 空界面，错误界面 点击重试时触发的方法，如果有使用 状态布局的话，一般子类都要实现
     */
    override fun onLoadRetry() {}

    /**
     * 显示 成功状态界面
     */
    override fun showSuccessUi() {
        uiStatusManger.showSuccess()
    }

    /**
     * 显示 错误 状态界面
     */
    override fun showErrorUi(errMessage: String) {
        uiStatusManger.showCallback(ErrorCallback::class.java)
    }


    /**
     * 显示 错误 状态界面
     */
    override fun showEmptyUi() {
        uiStatusManger.showCallback(EmptyCallback::class.java)
    }

    /**
     * 显示 loading 状态界面
     */
    override fun showLoadingUi() {
        uiStatusManger.showCallback(LoadingCallback::class.java)
    }

    /**
     * 显示自定义loading
     */
    override fun showCustomLoading(setting: LoadingDialogEntity) {
        showLoadingExt(setting.loadingMessage)
    }

    /**
     * 隐藏自定义loading
     */
    override fun dismissCustomLoading(setting: LoadingDialogEntity) {
        dismissLoadingExt()
    }
}