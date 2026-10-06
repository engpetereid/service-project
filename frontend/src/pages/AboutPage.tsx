import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import {
  Phone,
  Mail,
  Calendar,
  Sparkles,
  HeartHandshake,
  CalendarCheck,
  UserCheck,
  ShieldCheck,
  ArrowRight,
  Code2,
  ExternalLink,
  MessageCircle,
} from 'lucide-react';
import waznaLogo from '../assets/waznaLogo.png';

const GithubIcon: React.FC<{ className?: string }> = ({ className = 'w-4 h-4' }) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
    <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
  </svg>
);

const LinkedinIcon: React.FC<{ className?: string }> = ({ className = 'w-4 h-4' }) => (
  <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
    <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z"/>
  </svg>
);

interface AboutPageProps {
  isPublic?: boolean;
}

export const AboutPage: React.FC<AboutPageProps> = ({ isPublic = false }) => {
  const { isAuthenticated } = useAuth();
  const showPublicHeader = isPublic || !isAuthenticated;

  const phone = '01271970828';
  const email = 'eng.petereid@gmail.com';
  const githubUrl = 'https://github.com/engpetereid';
  const linkedinUrl = 'https://www.linkedin.com/in/peter-eid-449a2620b/';
  const whatsappUrl = `https://wa.me/201271970828?text=${encodeURIComponent('')}`;

  return (
    <div className="space-y-8 pb-12 animate-in fade-in">
      {/* Public Header Navigation (if viewed before logging in) */}
      {showPublicHeader && (
        <div className="bg-white border-b border-gray-100 py-3 px-4 sm:px-8 mb-6 -mx-4 sm:-mx-6 lg:-mx-8">
          <div className="max-w-5xl mx-auto flex items-center justify-between">
            <Link to="/" className="flex items-center gap-2.5">
              <img
                src={waznaLogo}
                alt="وزنة"
                className="w-10 h-10 rounded-xl object-cover shadow-sm border border-gray-100 shrink-0"
              />
              <div>
                <span className="font-black text-gray-900 text-lg leading-none">وزنة</span>
                <span className="text-[10px] text-gray-400 block font-medium">نظام خدمة الكنيسة</span>
              </div>
            </Link>

            <Link to="/login">
              <Button variant="primary" size="sm" className="font-bold text-xs">
                <span>تسجيل الدخول</span>
                <ArrowRight className="w-3.5 h-3.5 mr-1" />
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* Hero Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary-900 via-primary-800 to-indigo-950 text-white p-6 sm:p-10 shadow-xl shadow-primary-950/10">
        <div className="absolute top-0 left-0 w-96 h-96 bg-primary-600/10 rounded-full blur-3xl pointer-events-none -translate-x-1/2 -translate-y-1/2" />
        <div className="absolute bottom-0 right-0 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none translate-x-1/3 translate-y-1/3" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="max-w-2xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-bold text-primary-200 border border-white/10">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" />
              <span>برنامج وزنة • Wazna Church Management System</span>
            </div>

            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight">
              نظام <span className="text-amber-300">وزنة</span> لإدارة وخدمة ومتابعة الكنيسة
            </h1>

            <p className="text-sm sm:text-base text-primary-100/90 leading-relaxed max-w-2xl font-medium">
              منظومة رقمية متكاملة صُممت لتمكين الخدام وأمناء الخدمة من أداء رسالتهم الكنسية بأعلى درجات الأمانة والتنظيم، ومتابعة نمو المخدومين وافتقادهم الأسبوعي.
            </p>
          </div>

          <div className="shrink-0 hidden md:block">
            <img
              src={waznaLogo}
              alt="شعار وزنة"
              className="w-28 h-28 rounded-3xl object-cover shadow-2xl border-2 border-white/20 ring-4 ring-white/10"
            />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Developer Profile Card */}
        <Card className="lg:col-span-2 p-6 sm:p-8 bg-white border-gray-100 shadow-sm flex flex-col justify-between space-y-6">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-gray-100">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-600 via-primary-700 to-indigo-800 text-white flex items-center justify-center font-black text-2xl shadow-lg shadow-primary-600/20">
                  PE
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl sm:text-2xl font-black text-gray-900">المهندس بيتر عيد</h2>
                    <span className="px-2 py-0.5 rounded-full bg-primary-50 text-primary-700 text-[11px] font-bold border border-primary-100">
                      Developer
                    </span>
                  </div>
                  <p className="text-sm font-bold text-primary-600 flex items-center gap-1.5 mt-0.5">
                    <Code2 className="w-4 h-4" />
                    <span>Full Stack Developer</span>
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 bg-gray-50 px-3 py-1.5 rounded-xl border border-gray-100 self-start sm:self-auto">
                <Calendar className="w-4 h-4 text-primary-600" />
                <span>أول نسخة: أكتوبر 2026 (2026/10)</span>
              </div>
            </div>

            {/* About note */}
            <p className="text-sm text-gray-600 leading-relaxed font-medium">
              تم تطوير نظام <strong className="text-gray-900 font-black">«وزنة»</strong> بأحدث التقنيات البرمجية لتوفير تجربة استخدام سريعة وممتعة وسلسة لكافة الخدام والمسؤولين، مع مراعاة أعلى معايير أمان البيانات والسرية والسهولة في العمل من الهواتف المحمولة وأجهزة الكمبيوتر.
            </p>

            {/* Contact Actions Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              {/* Phone Direct */}
              <a
                href={`tel:${phone}`}
                className="flex items-center justify-between p-3.5 rounded-2xl border border-gray-200/80 hover:border-primary-500 hover:bg-primary-50/30 transition group min-h-[52px]"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-primary-600 group-hover:text-white transition">
                    <Phone className="w-4 h-4" />
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] text-gray-400 block font-medium">الهاتف المباشر</span>
                    <span className="text-sm font-bold font-mono text-gray-900" dir="ltr">
                      {phone}
                    </span>
                  </div>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-primary-600 transition" />
              </a>

              {/* WhatsApp */}
              <a
                href={whatsappUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-3.5 rounded-2xl border border-gray-200/80 hover:border-emerald-500 hover:bg-emerald-50/30 transition group min-h-[52px]"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition">
                    <MessageCircle className="w-4 h-4" />
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] text-gray-400 block font-medium">مراسلة واتساب</span>
                    <span className="text-sm font-bold font-mono text-gray-900" dir="ltr">
                      {phone}
                    </span>
                  </div>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-emerald-600 transition" />
              </a>

              {/* Email */}
              <a
                href={`mailto:${email}`}
                className="flex items-center justify-between p-3.5 rounded-2xl border border-gray-200/80 hover:border-amber-500 hover:bg-amber-50/30 transition group min-h-[52px]"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition">
                    <Mail className="w-4 h-4" />
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] text-gray-400 block font-medium">البريد الإلكتروني</span>
                    <span className="text-xs font-bold font-mono text-gray-900 truncate max-w-[180px] sm:max-w-none block">
                      {email}
                    </span>
                  </div>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-amber-600 transition" />
              </a>

              {/* LinkedIn */}
              <a
                href={linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-3.5 rounded-2xl border border-gray-200/80 hover:border-blue-600 hover:bg-blue-50/30 transition group min-h-[52px]"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition">
                    <LinkedinIcon className="w-4 h-4" />
                  </div>
                  <div className="text-right">
                    <span className="text-[11px] text-gray-400 block font-medium">لينكد إن (LinkedIn)</span>
                    <span className="text-xs font-bold text-gray-900 block">
                      peter-eid
                    </span>
                  </div>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-blue-600 transition" />
              </a>
            </div>

            {/* GitHub Button */}
            <div className="pt-2">
              <a
                href={githubUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full flex items-center justify-center gap-2 p-3 rounded-2xl bg-gray-900 hover:bg-gray-800 text-white font-bold text-xs transition shadow-sm"
              >
                <GithubIcon className="w-4 h-4" />
                <span>زيارة صفحة المطور على GitHub (@engpetereid)</span>
                <ExternalLink className="w-3.5 h-3.5 text-gray-400" />
              </a>
            </div>
          </div>
        </Card>

        {/* System Specs & Version Card */}
        <Card className="p-6 sm:p-8 bg-gradient-to-br from-white via-primary-50/10 to-indigo-50/20 border-gray-100 shadow-sm flex flex-col justify-between space-y-6">
          <div>
            <div className="w-12 h-12 rounded-2xl bg-primary-600 text-white flex items-center justify-center font-black text-xl mb-4 shadow-md shadow-primary-500/20">
              ✝
            </div>

            <h3 className="text-xl font-black text-gray-900">برنامج وزنة</h3>
            <p className="text-xs text-primary-700 font-bold mt-0.5">Wazna Church System</p>

            <div className="space-y-3 mt-6 text-xs text-gray-600 divide-y divide-gray-100">
              <div className="flex items-center justify-between pt-2">
                <span className="text-gray-400 font-medium">رقم الإصدار:</span>
                <span className="font-bold font-mono text-gray-900 bg-gray-100 px-2 py-0.5 rounded-md">
                  v1.0.0
                </span>
              </div>
              <div className="flex items-center justify-between pt-3">
                <span className="text-gray-400 font-medium">تاريخ أول نسخة:</span>
                <span className="font-bold text-gray-900">أكتوبر 2026 (2026/10)</span>
              </div>
              <div className="flex items-center justify-between pt-3">
                <span className="text-gray-400 font-medium">البيئة البرمجية:</span>
                <span className="font-bold text-gray-900">Spring Boot & React TS</span>
              </div>
              <div className="flex items-center justify-between pt-3">
                <span className="text-gray-400 font-medium">حالة النظام:</span>
                <span className="font-bold text-emerald-600 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block animate-pulse" />
                  مستقر ومفعّل للخدمة
                </span>
              </div>
            </div>
          </div>

          <div className="p-3.5 rounded-2xl bg-amber-50/80 border border-amber-200 text-amber-900 text-xs leading-relaxed font-medium">
            💡 لأي استفسارات تقنية أو اقتراحات تطويرية لخدمة الكنيسة، يُرجى التواصل مباشرة مع المطور عبر الهاتف أو الواتساب.
          </div>
        </Card>
      </div>

      {/* Program Core Features */}
      <div className="space-y-4 pt-4">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary-600" />
          <h3 className="text-lg font-black text-gray-900">ركائز منظومة «وزنة» في خدمة الكنيسة</h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-5 border-gray-100 hover:border-primary-200 transition">
            <div className="w-10 h-10 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mb-3">
              <CalendarCheck className="w-5 h-5" />
            </div>
            <h4 className="font-bold text-gray-900 text-sm mb-1">الافتقاد الأسبوعي الدوري</h4>
            <p className="text-xs text-gray-500 leading-relaxed">
              توزيع المخدومين على الخدام ومتابعة نسب الافتقاد أسبوعياً بالزيارات والمكالمات.
            </p>
          </Card>

          <Card className="p-5 border-gray-100 hover:border-primary-200 transition">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3">
              <UserCheck className="w-5 h-5" />
            </div>
            <h4 className="font-bold text-gray-900 text-sm mb-1">سجلات الحضور والغياب</h4>
            <p className="text-xs text-gray-500 leading-relaxed">
              رصد الحضور في القداسات واجتماعات الخدمة وتنبيهات الغياب المتتالي التلقائية.
            </p>
          </Card>

          <Card className="p-5 border-gray-100 hover:border-primary-200 transition">
            <div className="w-10 h-10 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center mb-3">
              <HeartHandshake className="w-5 h-5" />
            </div>
            <h4 className="font-bold text-gray-900 text-sm mb-1">جلسات وسجل الاعترافات</h4>
            <p className="text-xs text-gray-500 leading-relaxed">
              توثيق جلسات الاعتراف مع الآباء الكهنة ومتابعة الانتظام الروحي بكل سرية وخصوصية.
            </p>
          </Card>

          <Card className="p-5 border-gray-100 hover:border-primary-200 transition">
            <div className="w-10 h-10 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mb-3">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h4 className="font-bold text-gray-900 text-sm mb-1">صلاحيات وهيكل كنسي دقيق</h4>
            <p className="text-xs text-gray-500 leading-relaxed">
              فصل الصلاحيات بدقة بين الأمين العام، أمناء الخدمات، أمناء الفصول، والخدام.
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
};
