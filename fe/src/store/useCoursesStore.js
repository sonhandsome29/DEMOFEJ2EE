import { useMemo, useState } from 'react';
import { coursesApi } from '../api/courses.api';

export function useCoursesStore() {
  const [courses, setCourses] = useState([]);
  const [featured, setFeatured] = useState([]);
  const [categories, setCategories] = useState([]);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadCourses = async (params = {}) => {
    setLoading(true);
    try {
      const response = await coursesApi.getCourses(params);
      setCourses(response?.data?.courses || []);
      return { ok: true };
    } catch (error) {
      return { ok: false, message: error.message };
    } finally {
      setLoading(false);
    }
  };

  const loadFeatured = async () => {
    const response = await coursesApi.getFeatured();
    setFeatured(response?.data || []);
  };

  const loadCategories = async () => {
    const response = await coursesApi.getCategories();
    setCategories(response?.data || []);
  };

  const loadDetail = async (slug) => {
    const response = await coursesApi.getBySlug(slug);
    setDetail(response?.data || null);
    return response?.data || null;
  };

  return useMemo(
    () => ({ courses, featured, categories, detail, loading, loadCourses, loadFeatured, loadCategories, loadDetail }),
    [courses, featured, categories, detail, loading],
  );
}
