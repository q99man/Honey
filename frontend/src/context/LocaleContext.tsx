import React, { useCallback, useMemo, useState, useEffect, type ReactNode } from 'react';
import ko from '../locales/ko.json';
import en from '../locales/en.json';
import ja from '../locales/ja.json';
import { LocaleContext, type LocaleType } from './locale';

const translationByLocale: Record<LocaleType, Record<string, string>> = { ko, en, ja };

export const LocaleProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [locale, setLocale] = useState<LocaleType>(() => {
    const saved = localStorage.getItem('honeytong_locale');
    if (saved === 'ko' || saved === 'en' || saved === 'ja') {
      return saved as LocaleType;
    }
    return 'ko';
  });

  useEffect(() => {
    localStorage.setItem('honeytong_locale', locale);
  }, [locale]);

  const translations = translationByLocale[locale];

  const changeLanguage = useCallback((newLocale: LocaleType) => {
    setLocale(newLocale);
  }, []);

  const value = useMemo(
    () => ({ locale, changeLanguage, translations }),
    [changeLanguage, locale, translations],
  );

  return (
    <LocaleContext.Provider value={value}>
      {children}
    </LocaleContext.Provider>
  );
};
