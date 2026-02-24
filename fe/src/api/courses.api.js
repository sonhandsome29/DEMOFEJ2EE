import { request } from './http';

export const coursesApi = {
  getCourses: (params = {}) => {
    const safeSize = Math.min(Math.max(Number(params.size || 12), 1), 100);
    const safePage = Math.max(Number(params.page || 0), 0);
    return request('/courses', { params: { ...params, size: safeSize, page: safePage } });
  },
  getFeatured: () => request('/courses/featured'),
  getBySlug: (slug) => request(`/courses/${slug}`),
  getCategories: () => request('/courses/categories'),
};
