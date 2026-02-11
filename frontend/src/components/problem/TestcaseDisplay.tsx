import type { Testcase } from '@/types';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

interface TestcaseDisplayProps {
  testcases: Testcase[];
}

export function TestcaseDisplay({ testcases }: TestcaseDisplayProps) {
  const visibleTestcases = testcases.filter((tc) => !tc.isHidden);

  if (visibleTestcases.length === 0) {
    return (
      <div className="text-sm text-muted-foreground p-4">
        No example test cases available.
      </div>
    );
  }

  return (
    <Tabs defaultValue="0" className="w-full">
      <TabsList className="w-full justify-start">
        {visibleTestcases.map((_, index) => (
          <TabsTrigger key={index} value={String(index)}>
            Example {index + 1}
          </TabsTrigger>
        ))}
      </TabsList>
      {visibleTestcases.map((tc, index) => (
        <TabsContent key={index} value={String(index)} className="space-y-4 mt-4">
          <div>
            <h4 className="text-sm font-medium mb-2">Input</h4>
            <pre className="text-sm bg-muted p-3 rounded overflow-x-auto">
              {tc.input}
            </pre>
          </div>
          <div>
            <h4 className="text-sm font-medium mb-2">Expected Output</h4>
            <pre className="text-sm bg-muted p-3 rounded overflow-x-auto">
              {tc.expectedOutput}
            </pre>
          </div>
        </TabsContent>
      ))}
    </Tabs>
  );
}
