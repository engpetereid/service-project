import React, { useState, useEffect } from "react";
import { Download, Smartphone, X, Share, PlusSquare } from "lucide-react";
import waznaLogo from "../../assets/waznaLogo.png";

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
}

export const usePWAInstall = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isInstallable, setIsInstallable] = useState(false);
  const [isStandalone, setIsStandalone] = useState(false);
  const [isIOS, setIsIOS] = useState(false);

  useEffect(() => {
    // Check if app is already running as installed standalone PWA
    const standaloneMode =
      window.matchMedia("(display-mode: standalone)").matches ||
      (navigator as any).standalone === true ||
      document.referrer.includes("android-app://");
    setIsStandalone(standaloneMode);

    // Detect iOS
    const userAgent = window.navigator.userAgent.toLowerCase();
    const isIosDevice = /iphone|ipad|ipod/.test(userAgent);
    setIsIOS(isIosDevice);

    if (standaloneMode) return;

    // Listen for beforeinstallprompt event (Chrome, Android, Edge)
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setIsInstallable(true);
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);

    // If on iOS and not standalone, it is also installable via Safari share sheet
    if (isIosDevice && !standaloneMode) {
      setIsInstallable(true);
    }

    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    };
  }, []);

  const installApp = async (onIosPrompt?: () => void) => {
    if (isIOS) {
      if (onIosPrompt) onIosPrompt();
      return;
    }

    if (!deferredPrompt) return;

    try {
      await deferredPrompt.prompt();
      const choice = await deferredPrompt.userChoice;
      if (choice.outcome === "accepted") {
        setIsInstallable(false);
        setDeferredPrompt(null);
      }
    } catch (err) {
      console.warn("PWA install error:", err);
    }
  };

  return {
    isInstallable,
    isStandalone,
    isIOS,
    installApp,
  };
};

export const PWAInstallBanner: React.FC = () => {
  const { isInstallable, isStandalone, isIOS, installApp } = usePWAInstall();
  const [isDismissed, setIsDismissed] = useState(false);
  const [showIosModal, setShowIosModal] = useState(false);

  useEffect(() => {
    const dismissed = localStorage.getItem("wazna_pwa_banner_dismissed");
    if (dismissed === "true") {
      setIsDismissed(true);
    }
  }, []);

  if (isStandalone || !isInstallable || isDismissed) {
    return null;
  }

  const handleDismiss = () => {
    setIsDismissed(true);
    localStorage.setItem("wazna_pwa_banner_dismissed", "true");
  };

  const handleInstallClick = () => {
    if (isIOS) {
      setShowIosModal(true);
    } else {
      installApp();
    }
  };

  return (
    <>
      {/* Floating Bottom Card Banner for Mobile */}
      <div className="fixed bottom-16 sm:bottom-4 inset-x-3 sm:inset-x-auto sm:left-4 z-40 max-w-sm bg-white/95 backdrop-blur-md rounded-2xl border border-primary-100 shadow-2xl p-3.5 animate-in slide-in-from-bottom-5 duration-300">
        <div className="flex items-start gap-3">
          <img
            src={waznaLogo}
            alt="تطبيق وزنة"
            className="w-11 h-11 rounded-xl object-cover shadow-sm border border-gray-100 shrink-0"
          />

          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-1">
              <h4 className="text-xs font-bold text-gray-900 leading-tight">تثبيت تطبيق وزنة</h4>
              <button
                onClick={handleDismiss}
                className="text-gray-400 hover:text-gray-600 p-1 -mr-1 rounded-lg"
                aria-label="إغلاق"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <p className="text-[11px] text-gray-500 mt-0.5 leading-snug">
              نزّل التطبيق على هاتفك للوصول السريع بدون متصفح
            </p>

            <div className="mt-2.5 flex items-center gap-2">
              <button
                type="button"
                onClick={handleInstallClick}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-primary-600 hover:bg-primary-700 text-white text-xs font-bold shadow-sm shadow-primary-500/20 transition active:scale-95"
              >
                <Download className="w-3.5 h-3.5" />
                <span>تثبيت الآن</span>
              </button>

              <button
                type="button"
                onClick={handleDismiss}
                className="text-[11px] text-gray-400 hover:text-gray-600 font-medium px-2 py-1"
              >
                لاحقاً
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* iOS Instructions Modal */}
      {showIosModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl text-center space-y-4">
            <div className="w-16 h-16 mx-auto rounded-2xl overflow-hidden shadow-md border border-gray-100">
              <img src={waznaLogo} alt="تطبيق وزنة" className="w-full h-full object-cover" />
            </div>

            <h3 className="text-base font-black text-gray-900">تثبيت وزنة على الآيفون والآيباد</h3>

            <div className="text-xs text-gray-600 text-right space-y-3 bg-gray-50 p-4 rounded-2xl border border-gray-100">
              <div className="flex items-center gap-2.5">
                <span className="w-6 h-6 rounded-full bg-primary-100 text-primary-700 font-bold flex items-center justify-center shrink-0">
                  1
                </span>
                <span>
                  اضغط على زر المشاركة <Share className="w-4 h-4 inline-block text-primary-600 mx-1" /> في شريط سفلي في Safari.
                </span>
              </div>

              <div className="flex items-center gap-2.5">
                <span className="w-6 h-6 rounded-full bg-primary-100 text-primary-700 font-bold flex items-center justify-center shrink-0">
                  2
                </span>
                <span>
                  اختر <span className="font-bold text-gray-900">«إضافة إلى الشاشة الرئيسية»</span>{" "}
                  <PlusSquare className="w-4 h-4 inline-block text-primary-600 mx-1" />
                </span>
              </div>

              <div className="flex items-center gap-2.5">
                <span className="w-6 h-6 rounded-full bg-primary-100 text-primary-700 font-bold flex items-center justify-center shrink-0">
                  3
                </span>
                <span>اضغط «إضافة» (Add) وسيظهر أيقونة التطبيق على شاشة هاتفك فوراً!</span>
              </div>
            </div>

            <button
              onClick={() => setShowIosModal(false)}
              className="w-full py-2.5 rounded-xl bg-primary-600 text-white font-bold text-xs"
            >
              فهمت، حسناً
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export const PWAInstallSidebarButton: React.FC = () => {
  const { isInstallable, isStandalone, isIOS, installApp } = usePWAInstall();
  const [showIosModal, setShowIosModal] = useState(false);

  if (isStandalone || !isInstallable) return null;

  return (
    <>
      <button
        type="button"
        onClick={() => (isIOS ? setShowIosModal(true) : installApp())}
        className="w-full mx-auto my-2 p-3 rounded-2xl bg-gradient-to-r from-primary-50 via-sky-50 to-indigo-50 border border-primary-200/80 hover:border-primary-300 flex items-center gap-3 transition shadow-xs group text-right"
      >
        <div className="w-9 h-9 rounded-xl bg-primary-600 text-white flex items-center justify-center shrink-0 shadow-sm shadow-primary-500/20 group-hover:scale-105 transition">
          <Smartphone className="w-5 h-5" />
        </div>
        <div className="flex-1 min-w-0">
          <span className="block text-xs font-bold text-gray-900 leading-tight">تثبيت التطبيق على الهاتف</span>
          <span className="block text-[10px] text-gray-500 font-medium mt-0.5">وصول فوري وشاشة كاملة</span>
        </div>
        <Download className="w-4 h-4 text-primary-600 shrink-0" />
      </button>

      {showIosModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl text-center space-y-4">
            <img src={waznaLogo} alt="تطبيق وزنة" className="w-16 h-16 mx-auto rounded-2xl object-cover shadow-md" />
            <h3 className="text-base font-black text-gray-900">تثبيت وزنة على الآيفون</h3>
            <p className="text-xs text-gray-600">
              اضغط على زر المشاركة <Share className="w-3.5 h-3.5 inline mx-1 text-primary-600" /> في متصفح Safari، ثم اختر «إضافة إلى الشاشة الرئيسية» (Add to Home Screen).
            </p>
            <button
              onClick={() => setShowIosModal(false)}
              className="w-full py-2.5 rounded-xl bg-primary-600 text-white font-bold text-xs"
            >
              تم
            </button>
          </div>
        </div>
      )}
    </>
  );
};