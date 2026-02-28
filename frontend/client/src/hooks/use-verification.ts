import { useMutation } from "@tanstack/react-query";
import { api, type VerificationResultResponse } from "@shared/routes";

// Use the same API base URL logic as in api.js
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

export function useVerifyIdentity() {
  return useMutation({
    mutationFn: async (file: File): Promise<VerificationResultResponse> => {
      // Create FormData to handle the file upload properly
      const formData = new FormData();
      formData.append("file", file);

      // Using raw fetch here instead of apiRequest wrapper to easily handle FormData
      // which automatically sets the correct boundary headers.
      const res = await fetch(`${API_BASE_URL}/verify`, {
        method: api.verification.verify.method,
        body: formData,
        // Omit Content-Type header so the browser sets it with the boundary automatically
      });

      if (!res.ok) {
        let errorMessage = "Verification failed";
        try {
          const errorData = await res.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          errorMessage = res.statusText || errorMessage;
        }
        throw new Error(errorMessage);
      }

      // Parse the response using the zod schema defined in shared/routes.ts
      const data = await res.json();
      return api.verification.verify.responses[200].parse(data);
    },
  });
}
