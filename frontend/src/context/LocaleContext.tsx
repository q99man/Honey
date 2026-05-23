import React, { createContext, useState, useEffect, type ReactNode } from 'react';
import ko from '../locales/ko.json';
import en from '../locales/en.json';
import ja from '../locales/ja.json';

export type LocaleType = 'ko' | 'en' | 'ja';

interface LocaleContextProps {
  locale: LocaleType;
  changeLanguage: (locale: LocaleType) => void;
  translations: Record<string, string>;
}

export const LocaleContext = createContext<LocaleContextProps | undefined>(undefined);

export const LocaleProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [locale, setLocale] = useState<LocaleType>(() => {
    const saved = localStorage.getItem('honeytong_locale');
    if (saved === 'ko' || saved === 'en' || saved === 'ja') {
      return saved as LocaleType;
    }
    return 'ko';
  });

  const [translations, setTranslations] = useState<Record<string, string>>(ko);

  useEffect(() => {
    localStorage.setItem('honeytong_locale', locale);
    if (locale === 'en') {
      setTranslations(en);
    } else if (locale === 'ja') {
      setTranslations(ja);
    } else {
      setTranslations(ko);
    }
  }, [locale]);

  const changeLanguage = (newLocale: LocaleType) => {
    setLocale(newLocale);
  };

  return (
    <LocaleContext.Provider value={{ locale, changeLanguage, translations }}>
      {children}
    </LocaleContext.Provider>
  );
};
