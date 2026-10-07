import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from '../types/common.types';

export const TOKEN_KEY = 'service_project_token';

export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      // Clear token and signal session expiry
      localStorage.removeItem(TOKEN_KEY);
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    }

    const apiError: unknown = error.response?.data;
    let message = 'حدث خطأ في الاتصال بالخادم';
    if (typeof apiError === 'string' && apiError.trim().length > 0) {
      message = apiError;
    } else if (apiError && typeof apiError === 'object') {
      const errObj = apiError as Record<string, unknown>;
      if (errObj.fieldErrors && typeof errObj.fieldErrors === 'object') {
        const fieldErrors = errObj.fieldErrors as Record<string, string>;
        const errorList = Object.values(fieldErrors).filter((msg) => typeof msg === 'string' && msg.trim().length > 0);
        if (errorList.length > 0) {
          message = errorList.join(' - ');
        } else if (typeof errObj.message === 'string' && errObj.message.length > 0) {
          message = errObj.message;
        }
      } else if (typeof errObj.message === 'string' && errObj.message.length > 0) {
        message = errObj.message;
      }
    }
    return Promise.reject(new Error(message));
  }
);
