const specialCases = {
  aws: 'AWS',
  ui: 'UI',
  ux: 'UX',
  db: 'DB',
  js: 'JS',
};

export const formatCategoryTitle = (text) => {
  if (!text) return '';
  return text
    .replace(/[-_]/g, ' ')
    .split(' ')
    .map((word) => {
      const lowerWord = word.toLowerCase();
      return (
        specialCases[lowerWord] ||
        word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
      );
    })
    .join(' ');
};
