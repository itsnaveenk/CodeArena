import { toast as sonnerToast } from 'sonner';

type ToastOptions = Parameters<typeof sonnerToast>[1];
export type ToastId = string | number;

function ensureDismissAction(options: ToastOptions | undefined, id: ToastId): ToastOptions {
  if (options?.action) return options;

  return {
    ...options,
    action: {
      label: 'Dismiss',
      onClick: () => sonnerToast.dismiss(id),
    },
  } satisfies ToastOptions;
}

export const toast = {
  dismiss: (id?: ToastId) => sonnerToast.dismiss(id),

  dismissAll: () => sonnerToast.dismiss(),

  success: (message: string, options?: ToastOptions) => {
    const id = sonnerToast.success(message, options);
    sonnerToast.success(message, { ...ensureDismissAction(options, id), id } as any);
    return id;
  },

  error: (message: string, options?: ToastOptions) => {
    const id = sonnerToast.error(message, options);
    sonnerToast.error(message, { ...ensureDismissAction(options, id), id } as any);
    return id;
  },

  info: (message: string, options?: ToastOptions) => {
    const id = sonnerToast.info(message, options);
    sonnerToast.info(message, { ...ensureDismissAction(options, id), id } as any);
    return id;
  },

  warning: (message: string, options?: ToastOptions) => {
    const id = sonnerToast.warning(message, options);
    sonnerToast.warning(message, { ...ensureDismissAction(options, id), id } as any);
    return id;
  },

  message: (message: string, options?: ToastOptions) => {
    const id = sonnerToast(message, options);
    sonnerToast(message, { ...ensureDismissAction(options, id), id } as any);
    return id;
  },
};

export type { ToastOptions };
