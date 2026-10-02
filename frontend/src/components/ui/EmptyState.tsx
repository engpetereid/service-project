import React from 'react';
import { Card } from './Card';
import { Button } from './Button';
import { Link } from 'react-router-dom';

interface EmptyStateProps {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  actionUrl?: string;
  actionIcon?: React.ComponentType<{ className?: string }>;
  secondaryLabel?: string;
  onSecondaryAction?: () => void;
  secondaryUrl?: string;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction,
  actionUrl,
  actionIcon: ActionIcon,
  secondaryLabel,
  onSecondaryAction,
  secondaryUrl,
  className = '',
}) => {
  return (
    <Card className={`text-center py-16 px-6 max-w-lg mx-auto ${className}`}>
      <div className="w-16 h-16 rounded-2xl bg-primary-50 text-primary-600 flex items-center justify-center mx-auto mb-4 shadow-sm border border-primary-100/50">
        <Icon className="w-8 h-8" />
      </div>

      <h3 className="text-lg font-black text-gray-900 mb-2 leading-tight">
        {title}
      </h3>

      <p className="text-xs text-gray-500 max-w-sm mx-auto mb-6 leading-relaxed">
        {description}
      </p>

      {(actionLabel || secondaryLabel) && (
        <div className="flex flex-wrap items-center justify-center gap-3">
          {actionLabel && (
            actionUrl ? (
              <Link to={actionUrl}>
                <Button variant="primary" size="md" className="font-bold shadow-sm">
                  {ActionIcon && <ActionIcon className="w-4 h-4 ml-1.5" />}
                  <span>{actionLabel}</span>
                </Button>
              </Link>
            ) : (
              <Button
                variant="primary"
                size="md"
                onClick={onAction}
                className="font-bold shadow-sm"
              >
                {ActionIcon && <ActionIcon className="w-4 h-4 ml-1.5" />}
                <span>{actionLabel}</span>
              </Button>
            )
          )}

          {secondaryLabel && (
            secondaryUrl ? (
              <Link to={secondaryUrl}>
                <Button variant="outline" size="md" className="font-bold">
                  {secondaryLabel}
                </Button>
              </Link>
            ) : (
              <Button
                variant="outline"
                size="md"
                onClick={onSecondaryAction}
                className="font-bold"
              >
                {secondaryLabel}
              </Button>
            )
          )}
        </div>
      )}
    </Card>
  );
};
