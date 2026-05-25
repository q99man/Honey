import { createContext } from 'react';

export type LocaleType = 'ko' | 'en' | 'ja';

export interface LocaleContextProps {
  locale: LocaleType;
  changeLanguage: (locale: LocaleType) => void;
  translations: Record<string, string>;
}

export const LocaleContext = createContext<LocaleContextProps | undefined>(undefined);
