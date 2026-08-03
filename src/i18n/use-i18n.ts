import { useInventoryStore } from '@/features/inventory/inventory-store';

import { translate, type TranslationKey } from './translations';

export function useI18n() {
  const language = useInventoryStore((state) => state.language);
  return {
    language,
    t: (key: TranslationKey, params?: Record<string, string | number>) =>
      translate(language, key, params),
  };
}
