import { z } from "zod";

export const verificationResultSchema = z.object({
  riskLevel: z.string(),
  riskScore: z.number(),
  explanation: z.array(z.string()),
  extractedData: z.object({
    name: z.string(),
    idNumber: z.string(),
    dob: z.string(),
  }),
});

export type VerificationResult = z.infer<typeof verificationResultSchema>;
