import { useMutation } from "@tanstack/react-query";
import { api, type VerificationResultResponse } from "@shared/routes";

// Correct API base URL
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD
    ? "http://18.212.249.8:8080"
    : "http://localhost:8080");

export function useVerifyIdentity() {
  return useMutation({
    mutationFn: async (file: File): Promise<VerificationResultResponse> => {

      const formData = new FormData();
      formData.append("file", file);

      const res = await fetch(`${API_BASE_URL}/api/verify`, {
        method: "POST",
        body: formData,
      });

      if (!res.ok) {
        let errorMessage = "Verification failed";

        try {
          const errorData = await res.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          errorMessage = res.statusText || errorMessage;
        }

        throw new Error(errorMessage);
      }

      const data = await res.json();
      return api.verification.verify.responses[200].parse(data);
    },
  });
}