-- Drop old text prefix index
ALTER TABLE place_search_documents DROP INDEX idx_place_search_documents_text;

-- Create Full-Text Index with CJK ngram parser
CREATE FULLTEXT INDEX idx_place_search_documents_fts ON place_search_documents(search_text) WITH PARSER ngram;
