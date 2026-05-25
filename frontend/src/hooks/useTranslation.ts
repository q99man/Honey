import { useContext } from 'react';
import { LocaleContext } from '../context/locale';

export const useTranslation = () => {
  const context = useContext(LocaleContext);
  if (!context) {
    throw new Error('useTranslation must be used within a LocaleProvider');
  }

  const { locale, changeLanguage, translations } = context;

  const t = (key: string): string => {
    return translations[key] || key;
  };

  return { t, locale, changeLanguage };
};
