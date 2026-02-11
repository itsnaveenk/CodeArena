import { Link } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { Button } from '@/components/ui/button';
import { Code2, Trophy, Users, Zap } from 'lucide-react';

export default function LandingPage() {
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="py-20 px-4">
        <div className="container mx-auto text-center max-w-4xl">
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight mb-6">
            Master Coding Through
            <span className="text-primary"> Practice</span>
          </h1>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Sharpen your programming skills with curated challenges. 
            Write code, get instant feedback, and track your progress.
          </p>
          <div className="flex gap-4 justify-center">
            {isAuthenticated ? (
              <Button size="lg" asChild>
                <Link to="/problems">Browse Problems</Link>
              </Button>
            ) : (
              <>
                <Button size="lg" asChild>
                  <Link to="/signup">Get Started</Link>
                </Button>
                <Button size="lg" variant="outline" asChild>
                  <Link to="/login">Sign In</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-4 bg-muted/50">
        <div className="container mx-auto max-w-6xl">
          <h2 className="text-3xl font-bold text-center mb-12">
            Everything you need to level up
          </h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            <FeatureCard
              icon={<Code2 className="h-8 w-8" />}
              title="Multiple Languages"
              description="Write solutions in Java, Python, or C++. Pick your favorite."
            />
            <FeatureCard
              icon={<Zap className="h-8 w-8" />}
              title="Instant Feedback"
              description="Run your code against test cases and get results in seconds."
            />
            <FeatureCard
              icon={<Trophy className="h-8 w-8" />}
              title="Track Progress"
              description="Monitor your solve rate and see your improvement over time."
            />
            <FeatureCard
              icon={<Users className="h-8 w-8" />}
              title="Community Problems"
              description="Solve problems created by experienced developers."
            />
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4">
        <div className="container mx-auto text-center max-w-2xl">
          <h2 className="text-3xl font-bold mb-4">Ready to start coding?</h2>
          <p className="text-muted-foreground mb-8">
            Join CodeArena today and begin your journey to becoming a better programmer.
          </p>
          {!isAuthenticated && (
            <Button size="lg" asChild>
              <Link to="/signup">Create Free Account</Link>
            </Button>
          )}
        </div>
      </section>
    </div>
  );
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode; title: string; description: string }) {
  return (
    <div className="flex flex-col items-center text-center p-6">
      <div className="mb-4 text-primary">{icon}</div>
      <h3 className="font-semibold mb-2">{title}</h3>
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  );
}
