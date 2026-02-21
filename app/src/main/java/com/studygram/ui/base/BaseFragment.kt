package com.studygram.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.studygram.R
import com.studygram.utils.ErrorHandler

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    private var loadingDialog: AlertDialog? = null

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }

    abstract fun setupUI()

    abstract fun observeData()

    // Toast messages
    protected fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Snackbar messages
    protected fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, duration).show()
    }

    protected fun showSnackbarWithAction(
        message: String,
        actionText: String,
        action: () -> Unit,
        duration: Int = Snackbar.LENGTH_LONG
    ) {
        Snackbar.make(binding.root, message, duration)
            .setAction(actionText) { action() }
            .show()
    }

    // Error handling
    protected fun showError(message: String) {
        showSnackbar(message, Snackbar.LENGTH_LONG)
    }

    protected fun showError(exception: Throwable) {
        val message = ErrorHandler.getErrorMessage(exception)
        showError(message)

        // Show retry suggestion if available
        ErrorHandler.getRetrySuggestion(exception)?.let { suggestion ->
            showSnackbarWithAction(
                message = message,
                actionText = getString(R.string.retry),
                action = {
                    // Subclasses can override to implement retry logic
                    onRetryAfterError(exception)
                }
            )
        }
    }

    // Override this in subclasses to implement retry logic
    protected open fun onRetryAfterError(exception: Throwable) {
        // Default: just show a message
        showToast("Please try again")
    }

    // Loading dialog
    protected fun showLoading(message: String = getString(R.string.loading)) {
        hideLoading() // Dismiss any existing dialog

        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    protected fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    // Confirmation dialog
    protected fun showConfirmationDialog(
        title: String,
        message: String,
        positiveButtonText: String = getString(R.string.ok),
        negativeButtonText: String = getString(R.string.cancel),
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ -> onConfirm() }
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                onCancel?.invoke()
                dialog.dismiss()
            }
            .show()
    }

    // Success message
    protected fun showSuccess(message: String) {
        showSnackbar(message, Snackbar.LENGTH_SHORT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideLoading()
        _binding = null
    }
}
