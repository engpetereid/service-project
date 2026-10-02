import React, { HTMLAttributes } from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export const Card: React.FC<HTMLAttributes<HTMLDivElement>> = ({ children, className, ...props }) => {
  return (
    <div
      className={twMerge(
        clsx('bg-white rounded-2xl border border-gray-100 shadow-sm p-5 transition', className)
      )}
      {...props}
    >
      {children}
    </div>
  );
};
