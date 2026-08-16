package moe.brave.prune.hook;

import moe.brave.prune.compat.BraveSymbols;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

public final class WalletHooks {
    private static final Set<String> PROVIDER_CALLBACK_METHODS = Set.of(
            "showPanel",
            "unlockWallet",
            "showWalletBackup",
            "showWalletOnboarding",
            "showAccountCreation",
            "walletInteractionDetected");

    private WalletHooks() {}

    public static int install(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        int installed = installVoidMethods(
                module,
                loader,
                handles,
                BraveSymbols.WALLET_PROVIDER_DELEGATE_CLASSES,
                PROVIDER_CALLBACK_METHODS);
        installed += installWeb3NotificationGate(module, loader, handles);
        return installed;
    }

    private static int installVoidMethods(
            XposedModule module,
            ClassLoader loader,
            List<HookHandle> handles,
            List<String> classNames,
            Set<String> methodNames) {
        int installed = 0;
        Set<Method> hooked = new HashSet<>();
        for (String className : classNames) {
            try {
                Class<?> type = Class.forName(className, false, loader);
                for (Method method : type.getDeclaredMethods()) {
                    if (!methodNames.contains(method.getName())
                            || method.getReturnType() != void.class
                            || !hooked.add(method)) {
                        continue;
                    }
                    method.setAccessible(true);
                    handles.add(module.hook(method).intercept(chain -> null));
                    installed++;
                }
            } catch (ClassNotFoundException ignored) {
                // Product classes vary between Brave channels and releases.
            }
        }
        return installed;
    }

    private static int installWeb3NotificationGate(
            XposedModule module, ClassLoader loader, List<HookHandle> handles) {
        int installed = 0;
        for (String className : BraveSymbols.WALLET_PROVIDER_DELEGATE_CLASSES) {
            try {
                Class<?> type = Class.forName(className, false, loader);
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.getName().equals("isWeb3NotificationAllowed")
                            || method.getReturnType() != boolean.class
                            || method.getParameterCount() != 0) {
                        continue;
                    }
                    method.setAccessible(true);
                    handles.add(module.hook(method).intercept(chain -> false));
                    installed++;
                }
            } catch (ClassNotFoundException ignored) {
                // The provider helper is optional when Wallet is omitted from the build.
            }
        }
        return installed;
    }
}
